/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 ******************************************************************************/
package org.apache.sling.scripting.sightly.compiler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.scripting.sightly.compiler.backend.BackendCompiler;
import org.apache.sling.scripting.sightly.compiler.commands.CommandStream;
import org.apache.sling.scripting.sightly.impl.compiler.CompilationResultImpl;
import org.apache.sling.scripting.sightly.impl.compiler.CompilerMessageImpl;
import org.apache.sling.scripting.sightly.impl.compiler.PushStream;
import org.apache.sling.scripting.sightly.impl.compiler.Syntax;
import org.apache.sling.scripting.sightly.impl.compiler.debug.SanityChecker;
import org.apache.sling.scripting.sightly.impl.compiler.frontend.SimpleFrontend;
import org.apache.sling.scripting.sightly.impl.compiler.optimization.CoalescingWrites;
import org.apache.sling.scripting.sightly.impl.compiler.optimization.DeadCodeRemoval;
import org.apache.sling.scripting.sightly.impl.compiler.optimization.SequenceStreamTransformer;
import org.apache.sling.scripting.sightly.impl.compiler.optimization.StreamTransformer;
import org.apache.sling.scripting.sightly.impl.compiler.optimization.SyntheticMapRemoval;
import org.apache.sling.scripting.sightly.impl.compiler.optimization.UnusedVariableRemoval;
import org.apache.sling.scripting.sightly.impl.compiler.optimization.reduce.ConstantFolding;
import org.apache.sling.scripting.sightly.impl.filter.ExpressionContext;
import org.apache.sling.scripting.sightly.impl.filter.Filter;
import org.apache.sling.scripting.sightly.impl.filter.FormatFilter;
import org.apache.sling.scripting.sightly.impl.filter.I18nFilter;
import org.apache.sling.scripting.sightly.impl.filter.JoinFilter;
import org.apache.sling.scripting.sightly.impl.filter.URIManipulationFilter;
import org.apache.sling.scripting.sightly.impl.filter.XSSFilter;
import org.apache.sling.scripting.sightly.impl.plugin.AttributePlugin;
import org.apache.sling.scripting.sightly.impl.plugin.CallPlugin;
import org.apache.sling.scripting.sightly.impl.plugin.ElementPlugin;
import org.apache.sling.scripting.sightly.impl.plugin.IncludePlugin;
import org.apache.sling.scripting.sightly.impl.plugin.ListPlugin;
import org.apache.sling.scripting.sightly.impl.plugin.Plugin;
import org.apache.sling.scripting.sightly.impl.plugin.RepeatPlugin;
import org.apache.sling.scripting.sightly.impl.plugin.ResourcePlugin;
import org.apache.sling.scripting.sightly.impl.plugin.SetPlugin;
import org.apache.sling.scripting.sightly.impl.plugin.TemplatePlugin;
import org.apache.sling.scripting.sightly.impl.plugin.TestPlugin;
import org.apache.sling.scripting.sightly.impl.plugin.TextPlugin;
import org.apache.sling.scripting.sightly.impl.plugin.UnwrapPlugin;
import org.apache.sling.scripting.sightly.impl.plugin.UsePlugin;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;

/**
 * <p>
 * The {@link SightlyCompiler} interprets a HTL script and transforms it internally into a {@link CommandStream}. The
 * {@link CommandStream} can be fed to a {@link BackendCompiler} for transforming the stream into executable code, either by
 * transpiling the commands to a JVM supported language or by directly executing them.
 * </p>
 */
@Component(
        service = SightlyCompiler.class
)
public final class SightlyCompiler {

    private final StreamTransformer optimizer;
    private final SimpleFrontend frontend;
    private final Set<String> knownExpressionOptions;
    private final List<Plugin> plugins;
    private final List<Filter> filters;

    public SightlyCompiler() {
        this(Collections.emptySet());
    }

    private SightlyCompiler(Set<String> additionalExpresionOptions) {
        ArrayList<StreamTransformer> transformers = new ArrayList<>(5);
        transformers.add(ConstantFolding.transformer());
        transformers.add(DeadCodeRemoval.transformer());
        transformers.add(SyntheticMapRemoval.TRANSFORMER);
        transformers.add(UnusedVariableRemoval.TRANSFORMER);
        transformers.add(CoalescingWrites.TRANSFORMER);
        optimizer = new SequenceStreamTransformer(transformers);

        // register plugins
        plugins = new ArrayList<>(12);
        plugins.add(new AttributePlugin());
        plugins.add(new CallPlugin());
        plugins.add(new ElementPlugin());
        plugins.add(new IncludePlugin());
        plugins.add(new ListPlugin());
        plugins.add(new RepeatPlugin());
        plugins.add(new ResourcePlugin());
        plugins.add(new TemplatePlugin());
        plugins.add(new TestPlugin());
        plugins.add(new SetPlugin());
        plugins.add(new TextPlugin());
        plugins.add(new UnwrapPlugin());
        plugins.add(new UsePlugin());
        Collections.sort(plugins);

        // register filters
        filters = new ArrayList<>(5);
        filters.add(I18nFilter.getInstance());
        filters.add(FormatFilter.getInstance());
        filters.add(JoinFilter.getInstance());
        filters.add(URIManipulationFilter.getInstance());
        filters.add(XSSFilter.getInstance());
        Collections.sort(filters);
        knownExpressionOptions = new HashSet<>(additionalExpresionOptions);
        for (Filter filter : filters) {
            knownExpressionOptions.addAll(filter.getOptions());
        }
        knownExpressionOptions.add(Syntax.CONTEXT_OPTION);
        for (ExpressionContext context : ExpressionContext.values()) {
            knownExpressionOptions.addAll(context.getOptions());
        }
        frontend = new SimpleFrontend(plugins, filters, knownExpressionOptions);
    }

    /**
     * <p>
     * Returns an instance of the {@code SightlyCompiler} with the provided {@code options} added to the list of known expression options.
     * </p>
     * <p>
     * The compiler builds internally a set of allowed options from the options permitted by the expressions or plugins. As soon as an
     * expression contains an unknown option the compiler logs a warning. Since the compiler works with dynamically registered
     * {@link org.apache.sling.scripting.sightly.compiler.expression.nodes.RuntimeCall}s, some of them can work with additional expression
     * options, not known to the compiler.
     * </p>
     * <p>
     * <strong>NOTE</strong>: The {@code data-sly-template, data-sly-call, data-sly-use} plugins allow arbitrary options which define
     * parameters and do not generate warnings.
     * </p>
     *
     * @param options the options to add to the compiler's set of known expression options
     * @return an instance of the compiler
     */
    public static SightlyCompiler withKnownExpressionOptions(@NotNull Set<String> options) {
        return new SightlyCompiler(options);
    }

    /**
     * Compiles a {@link CompilationUnit}.
     *
     * @param compilationUnit a compilation unit
     * @return the compilation result
     */
    public CompilationResult compile(CompilationUnit compilationUnit) {
        return compile(compilationUnit, null);
    }

    /**
     * Compiles a {@link CompilationUnit}, passing the processed {@link CommandStream} to the provided {@link BackendCompiler}.
     *
     * @param compilationUnit a compilation unit
     * @param backendCompiler the backend compiler
     * @return the compilation result
     */
    public CompilationResult compile(CompilationUnit compilationUnit, BackendCompiler backendCompiler) {
        String scriptName = compilationUnit.getScriptName();
        String scriptSource = null;
        PushStream stream = new PushStream();
        SanityChecker.attachChecker(stream);
        CommandStream optimizedStream = optimizer.transform(stream);
        CompilationResultImpl compilationResult = new CompilationResultImpl(optimizedStream);
        try {
            scriptSource = IOUtils.toString(compilationUnit.getScriptReader());

            //optimizedStream.addHandler(LoggingHandler.INSTANCE);
            if (backendCompiler != null) {
                backendCompiler.handle(optimizedStream);
            }
            frontend.compile(stream, scriptSource);
            for (PushStream.StreamMessage w : stream.getWarnings()) {
                ScriptError warning = getScriptError(scriptSource, w.getCode(), 1, 0, w.getMessage());
                compilationResult.getWarnings().add(new CompilerMessageImpl(scriptName, warning.errorMessage, warning.lineNumber, warning
                        .column));
            }
        } catch (SightlyCompilerException e) {
            ScriptError scriptError = getScriptError(scriptSource, e.getOffendingInput(), e.getLine(), e.getColumn(), e.getMessage());
            compilationResult.getErrors().add(new CompilerMessageImpl(scriptName, scriptError.errorMessage, scriptError.lineNumber,
                    scriptError.column));
        } catch (IOException e) {
            throw new SightlyCompilerException("Unable to read source code from CompilationUnit identifying script " + scriptName, e);
        }
        compilationResult.seal();
        return compilationResult;
    }

    private ScriptError getScriptError(String documentFragment, String offendingInput, int lineOffset, int columnOffset, String message) {
        if (StringUtils.isNotEmpty(offendingInput)) {
            String longestContiguousOffendingSequence = null;
            if (documentFragment.contains(offendingInput)) {
                longestContiguousOffendingSequence = offendingInput;
            } else {
                longestContiguousOffendingSequence = getContiguousOffendingSequence(offendingInput);
            }
            int offendingInputIndex = documentFragment.indexOf(longestContiguousOffendingSequence);
            if (offendingInputIndex > -1) {
                String textBeforeError = documentFragment.substring(0, offendingInputIndex);
                int line = lineOffset;
                int lastNewLineIndex = 0;
                for (String s : new String[]{"\r\n", "\r", "\n"}) {
                    int l = textBeforeError.split(s, -1).length - 1;
                    if (l + lineOffset > line) {
                        line = l + lineOffset;
                        int ix = textBeforeError.lastIndexOf(s);
                        if (ix > 0) {
                            lastNewLineIndex = ix + s.length() - 1;
                        }
                    }
                }
                int column = textBeforeError.substring(lastNewLineIndex).length();
                if (column != columnOffset) {
                    column += columnOffset;
                }
                return new ScriptError(line, column, longestContiguousOffendingSequence + ": " + message);
            }
        }
        return new ScriptError(lineOffset, columnOffset, message);
    }

    private String getContiguousOffendingSequence(String input) {
        if (input != null) {
            StringBuilder longestSequence = new StringBuilder();
            char[] inputCharArray = input.toCharArray();
            for (int index = inputCharArray.length - 1; index >= 0; index--) {
                char c = inputCharArray[index];
                if (!Character.isWhitespace(c)) {
                    longestSequence.insert(0, c);
                } else {
                    break;
                }
            }
            return longestSequence.toString();
        }
        return null;
    }

    private static class ScriptError {

        private int lineNumber;
        private int column;
        private String errorMessage;

        ScriptError(int lineNumber, int column, String errorMessage) {
            this.lineNumber = lineNumber;
            this.column = column;
            this.errorMessage = errorMessage;
        }
    }
}
