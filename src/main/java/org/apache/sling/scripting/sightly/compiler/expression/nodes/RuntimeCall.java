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
package org.apache.sling.scripting.sightly.compiler.expression.nodes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.sling.scripting.sightly.compiler.SightlyCompilerException;
import org.apache.sling.scripting.sightly.compiler.expression.ExpressionNode;
import org.apache.sling.scripting.sightly.compiler.expression.NodeVisitor;

/**
 * A {@code RuntimeCall} is a special expression which provides access to utility functions from the runtime.
 */
public final class RuntimeCall implements ExpressionNode {

    /**
     * <p>
     *     The name of the {@link org.apache.sling.scripting.sightly.compiler.expression.nodes.RuntimeCall} function that will process string
     *     formatting. The function will receive the following parameters:
     * </p>
     * <ol>
     *     <li>the format String (e.g. 'Hello {0}, welcome to {1}')</li>
     *     <li>an array of objects that will replace the format placeholders</li>
     * </ol>
     * <p>
     *     For more details check https://github.com/Adobe-Marketing-Cloud/htl-spec/blob/1.2/SPECIFICATION.md#122-format.
     * </p>
     */
    public static final String FORMAT = "format";

    /**
     * <p>
     *     The name of the {@link org.apache.sling.scripting.sightly.compiler.expression.nodes.RuntimeCall} function that will process
     *     i18n. The function will receive the following parameters:
     * </p>
     * <ol>
     *     <li>the String to translate</li>
     *     <li>optional: locale information</li>
     *     <li>optional: hint information</li>
     *     <li>optional (not part of the specification): basename information; for more details see
     *     {@link java.util.ResourceBundle#getBundle(String, java.util.Locale)}</li>
     * </ol>
     * <p>
     *     For more details check https://github.com/Adobe-Marketing-Cloud/htl-spec/blob/1.2/SPECIFICATION.md#123-i18n.
     * </p>
     */
    public static final String I18N = "i18n";

    /**
     * <p>
     *     The name of the {@link org.apache.sling.scripting.sightly.compiler.expression.nodes.RuntimeCall} function that will process
     *     join operations on arrays. The function will receive the following parameters:
     * </p>
     * <ol>
     *     <li>the array of objects to join (e.g. [1, 2, 3])</li>
     *     <li>the join string (e.g. ';')</li>
     * </ol>
     * <p>
     *     For more details check https://github.com/Adobe-Marketing-Cloud/htl-spec/blob/1.2/SPECIFICATION.md#124-array-join.
     * </p>
     */
    public static final String JOIN = "join";

    /**
     * <p>
     *     The name of the {@link org.apache.sling.scripting.sightly.compiler.expression.nodes.RuntimeCall} function that will provide
     *     URI manipulation support. The function will receive the following parameters:
     * </p>
     * <ol>
     *     <li>optional: a URI string to process</li>
     *     <li>optional: a Map containing URI manipulation options</li>
     * </ol>
     * <p>
     *     For more details check https://github.com/Adobe-Marketing-Cloud/htl-spec/blob/1.2/SPECIFICATION.md#125-uri-manipulation.
     * </p>
     */
    public static final String URI_MANIPULATION = "uriManipulation";

    /**
     * <p>
     *     The name of the {@link org.apache.sling.scripting.sightly.compiler.expression.nodes.RuntimeCall} function that will provide
     *     XSS escaping and filtering support. The function will receive the following parameters:
     * </p>
     * <ol>
     *     <li>the original string to escape / filter</li>
     *     <li>the context to be applied - see {@link org.apache.sling.scripting.sightly.compiler.expression.MarkupContext}</li>
     * </ol>
     * <p>
     *     For more details check https://github.com/Adobe-Marketing-Cloud/htl-spec/blob/1.2/SPECIFICATION.md#121-display-context.
     * </p>
     */
    public static final String XSS = "xss";

    /**
     * <p>
     *     The name of the {@link org.apache.sling.scripting.sightly.compiler.expression.nodes.RuntimeCall} function that will perform
     *     script execution delegation. The function will receive the following parameters:
     * </p>
     * <ol>
     *     <li>optional: the relative or absolute path of the script to execute</li>
     *     <li>optional: a Map of options to perform script include processing</li>
     * </ol>
     * <p>
     *     For more details about the supported options check
     *     https://github.com/Adobe-Marketing-Cloud/htl-spec/blob/1.2/SPECIFICATION.md#228-include.
     * </p>
     */
    public static final String INCLUDE = "include";

    /**
     * <p>
     *     The name of the {@link org.apache.sling.scripting.sightly.compiler.expression.nodes.RuntimeCall} function that will perform
     *     resource inclusion in the rendering process. The function will receive the following parameters:
     * </p>
     * <ol>
     *     <li>optional: a relative or absolute path of the resource to be included</li>
     *     <li>optional: a Map containing the resource processing options</li>
     * </ol>
     * <p>
     *     For more details about the supported options check
     *     https://github.com/Adobe-Marketing-Cloud/htl-spec/blob/1.2/SPECIFICATION.md#229-resource.
     * </p>
     */
    public static final String RESOURCE = "includeResource";

    /**
     * <p>
     *     The name of the {@link org.apache.sling.scripting.sightly.compiler.expression.nodes.RuntimeCall} function that will provide
     *     the support for loading Use-API objects. The function will receive the following parameters:
     * </p>
     * <ol>
     *     <li>an identifier that allows to discover the Use-API object that needs to be loaded</li>
     *     <li>optional: a Map of the arguments that are passed to the Use-API object for initialisation or to provide context</li>
     * </ol>
     * <p>
     *     For more details check https://github.com/Adobe-Marketing-Cloud/htl-spec/blob/1.2/SPECIFICATION.md#221-use.
     * </p>
     */
    public static final String USE = "use";

    private static final Set<String> RUNTIME_FUNCTIONS;

    static {
        RUNTIME_FUNCTIONS = new HashSet<>();
        RUNTIME_FUNCTIONS.add(USE);
        RUNTIME_FUNCTIONS.add(RESOURCE);
        RUNTIME_FUNCTIONS.add(INCLUDE);
        RUNTIME_FUNCTIONS.add(I18N);
        RUNTIME_FUNCTIONS.add(XSS);
        RUNTIME_FUNCTIONS.add(URI_MANIPULATION);
        RUNTIME_FUNCTIONS.add(JOIN);
        RUNTIME_FUNCTIONS.add(FORMAT);
    }

    private final String functionName;
    private final List<ExpressionNode> arguments;


    /**
     * Creates a {@code RuntimeCall} based on a {@code functionName} and an array of {@code arguments}.
     *
     * @param functionName the name of the function identifying the runtime call
     * @param arguments    the arguments passed to the runtime call
     */
    public RuntimeCall(String functionName, ExpressionNode... arguments) {
        this(functionName, Arrays.asList(arguments));
    }

    /**
     * Creates a {@code RuntimeCall} based on a {@code functionName} and a list of {@code arguments}.
     *
     * @param functionName the name of the function identifying the runtime call
     * @param arguments    the arguments passed to the runtime call
     */
    public RuntimeCall(String functionName, List<ExpressionNode> arguments) {
        if (!RUNTIME_FUNCTIONS.contains(functionName)) {
            throw new SightlyCompilerException(
                    String.format("Function %s is not a recognised runtime function - %s.", functionName, RUNTIME_FUNCTIONS));
        }
        this.functionName = functionName;
        this.arguments = new ArrayList<>(arguments);
    }

    /**
     * Get the name of the runtime call.
     *
     * @return the name of the runtime call
     */
    public String getFunctionName() {
        return functionName;
    }

    /**
     * Get the nodes of the argument calls.
     *
     * @return the arguments list
     */
    public List<ExpressionNode> getArguments() {
        return Collections.unmodifiableList(arguments);
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.evaluate(this);
    }
}
