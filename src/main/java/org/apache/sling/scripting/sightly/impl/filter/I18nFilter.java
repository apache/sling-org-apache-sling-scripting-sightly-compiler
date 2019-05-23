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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.sling.scripting.sightly.compiler.expression.Expression;
import org.apache.sling.scripting.sightly.compiler.expression.ExpressionNode;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.MapLiteral;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.RuntimeCall;

/**
 * Filter for i18n translation
 */
public final class I18nFilter extends AbstractFilter {

    public static final String I18N_OPTION = "i18n";
    public static final String HINT_OPTION = "hint";
    public static final String LOCALE_OPTION = "locale";
    public static final String BASENAME_OPTION = "basename";

    private static final Set<String> OPTIONS = new HashSet<>(Arrays.asList(I18N_OPTION, HINT_OPTION, LOCALE_OPTION, BASENAME_OPTION));
    private static final Set<String> REQUIRED_OPTIONS = Collections.singleton(I18N_OPTION);

    private static final class I18nFilterLoader {
        private static final I18nFilter INSTANCE = new I18nFilter();
    }

    private I18nFilter() {
        priority = 90;
    }

    public static I18nFilter getInstance() {
        return I18nFilterLoader.INSTANCE;
    }

    @Override
    protected Expression apply(Expression expression, Map<String, ExpressionNode> options) {
        if (options.containsKey(I18N_OPTION)) {
            ExpressionNode translation = new RuntimeCall(RuntimeCall.I18N, expression.getRoot(), new MapLiteral(options));
            expression.removeOption(I18N_OPTION);
            expression.getOptions().put(FormatFilter.FORMAT_LOCALE_OPTION, options.get(LOCALE_OPTION));
            return expression.withNode(translation);
        }
        return expression;
    }

    @Override
    public Set<String> getOptions() {
        return OPTIONS;
    }

    @Override
    public Set<String> getRequiredOptions() {
        return REQUIRED_OPTIONS;
    }

    @Override
    public Set<ExpressionContext> getApplicableContexts() {
        return NON_PARAMETRIZABLE_CONTEXTS;
    }
}
