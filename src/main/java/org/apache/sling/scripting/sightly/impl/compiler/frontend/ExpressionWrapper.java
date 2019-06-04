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
package org.apache.sling.scripting.sightly.impl.compiler.frontend;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.apache.sling.scripting.sightly.compiler.expression.Expression;
import org.apache.sling.scripting.sightly.compiler.expression.ExpressionNode;
import org.apache.sling.scripting.sightly.compiler.expression.MarkupContext;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.BinaryOperation;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.BinaryOperator;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.StringConstant;
import org.apache.sling.scripting.sightly.impl.compiler.PushStream;
import org.apache.sling.scripting.sightly.impl.compiler.Syntax;
import org.apache.sling.scripting.sightly.impl.filter.AbstractFilter;
import org.apache.sling.scripting.sightly.impl.filter.ExpressionContext;
import org.apache.sling.scripting.sightly.impl.filter.Filter;

/**
 * This object wraps expressions in filter applications depending on options.
 */
public class ExpressionWrapper {

    private final List<Filter> filters;
    private final Set<String> knownOptions;
    private final PushStream stream;

    public ExpressionWrapper(PushStream stream, List<Filter> filters, Set<String> knownExpressionOptions) {
        this.stream = stream;
        this.filters = filters;
        this.knownOptions = knownExpressionOptions;
    }

    public Expression transform(Interpolation interpolation, MarkupContext markupContext, ExpressionContext expressionContext) {
        ArrayList<ExpressionNode> nodes = new ArrayList<>();
        HashMap<String, ExpressionNode> options = new HashMap<>();
        for (Fragment fragment : interpolation.getFragments()) {
            if (fragment.isString()) {
                nodes.add(new StringConstant(fragment.getText()));
            } else {
                Expression expression = fragment.getExpression();
                if (AbstractFilter.NON_PARAMETRIZABLE_CONTEXTS.contains(expressionContext)) {
                    expression.getOptions().keySet().stream().filter(option -> !knownOptions.contains(option)).forEach(
                        unknownOption ->
                        stream.warn(
                            new PushStream.StreamMessage(String.format("Unknown option '%s'.", unknownOption), expression.getRawText())
                        )
                    );
                }
                Expression transformed = adjustToContext(expression, markupContext, expressionContext);
                nodes.add(transformed.getRoot());
                options.putAll(transformed.getOptions());
            }
        }
        ExpressionNode root = join(nodes);
        if (interpolation.size() > 1) {
            //context must not be calculated by merging
            options.remove(Syntax.CONTEXT_OPTION);
        }
        return new Expression(root, options, interpolation.getContent());
    }

    private Expression applyFilters(Expression expression, ExpressionContext expressionContext) {
        Expression result = expression;
        for (Filter filter : filters) {
            result = filter.apply(result, expressionContext);
        }
        return result;
    }

    public Expression adjustToContext(Expression expression, MarkupContext context, ExpressionContext expressionContext) {
        if (context != null && !expression.containsOption(Syntax.CONTEXT_OPTION)) {
            expression.getOptions().put(Syntax.CONTEXT_OPTION, new StringConstant(context.getName()));
        }
        return applyFilters(expression, expressionContext);
    }

    private ExpressionNode join(List<ExpressionNode> nodes) {
        if (nodes.isEmpty()) {
            return StringConstant.EMPTY;
        }
        ExpressionNode root = nodes.remove(0);
        for (ExpressionNode node : nodes) {
            root = new BinaryOperation(BinaryOperator.CONCATENATE, root, node);
        }
        return root;
    }
}
