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
package org.apache.sling.scripting.sightly.impl.filter;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.sling.scripting.sightly.compiler.expression.Expression;
import org.apache.sling.scripting.sightly.compiler.expression.ExpressionNode;

public abstract class AbstractFilter implements Filter {

    protected int priority = 100;
    public static final Set<ExpressionContext> NON_PARAMETRIZABLE_CONTEXTS;
    static {
        Set<ExpressionContext> contexts = new HashSet<>(Arrays.asList(ExpressionContext.values()));
        contexts.remove(ExpressionContext.PLUGIN_DATA_SLY_USE);
        contexts.remove(ExpressionContext.PLUGIN_DATA_SLY_TEMPLATE);
        contexts.remove(ExpressionContext.PLUGIN_DATA_SLY_CALL);
        NON_PARAMETRIZABLE_CONTEXTS = Collections.unmodifiableSet(contexts);
    }

    private final Set<ExpressionContext> applicableContexts;
    private final Set<String> options;
    private final Set<String> requiredOptions;

    AbstractFilter(Set<ExpressionContext> applicableContexts, Set<String> options, Set<String> requiredOptions) {
        this.applicableContexts = applicableContexts;
        this.options = options;
        this.requiredOptions = requiredOptions;
    }

    @Override
    public int priority() {
        return priority;
    }

    @Override
    public int compareTo(Filter o) {
        if (this.priority < o.priority()) {
            return -1;
        } else if (this.priority == o.priority()) {
            return 0;
        }
        return 1;
    }

    @Override
    public Expression apply(Expression expression, ExpressionContext expressionContext) {
        Set<String> expressionOptions = expression.getOptions().keySet();
        if (getApplicableContexts().contains(expressionContext) && expressionOptions.containsAll(getRequiredOptions())) {
            return apply(expression, getFilterOptions(expression, getOptions()));
        }
        return expression;
    }

    protected abstract Expression apply(Expression expression, Map<String, ExpressionNode> options);

    /**
     * Collects the options passed in the {@code options} array into a new map while removing them from the original expression.
     *
     * @param expression the expression providing the options to be processed
     * @param options    the options of interest for the {@link Filter}
     * @return a map with the retrieved options; the map can be empty if none of the options were found
     */
    private Map<String, ExpressionNode> getFilterOptions(Expression expression, Set<String> options) {
        Map<String, ExpressionNode> collector = new HashMap<>();
        for (String option : options) {
            ExpressionNode optionNode = expression.removeOption(option);
            if (optionNode != null) {
                collector.put(option, optionNode);
            }
        }
        return collector;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && this.getClass().equals(obj.getClass());
    }

    @Override
    public Set<String> getOptions() {
        return options;
    }

    @Override
    public Set<String> getRequiredOptions() {
        return requiredOptions;
    }

    @Override
    public Set<ExpressionContext> getApplicableContexts() {
        return applicableContexts;
    }
}
