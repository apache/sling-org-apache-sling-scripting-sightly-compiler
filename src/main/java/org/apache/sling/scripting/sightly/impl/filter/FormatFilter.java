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
 * Implementation for the format filter &amp; runtime support.
 */
public class FormatFilter extends AbstractFilter {

    public static final String FORMAT_OPTION = "format";
    public static final String TYPE_OPTION = "type";
    public static final String FORMAT_LOCALE_OPTION = "formatLocale";
    public static final String TIMEZONE_OPTION = "timezone";

    private static final Set<String> OPTIONS = new HashSet<>(Arrays.asList(FORMAT_OPTION, TYPE_OPTION, FORMAT_LOCALE_OPTION, TIMEZONE_OPTION));
    private static final Set<String> REQUIRED_OPTIONS = Collections.singleton(FORMAT_OPTION);

    private static final class FormatFilterLoader {
        private static final FormatFilter INSTANCE = new FormatFilter();
    }

    private FormatFilter() {
    }

    public static FormatFilter getInstance() {
        return FormatFilterLoader.INSTANCE;
    }

    @Override
    protected Expression apply(Expression expression, Map<String, ExpressionNode> options) {
        ExpressionNode translation =
                new RuntimeCall(RuntimeCall.FORMAT, expression.getRoot(), new MapLiteral(getFilterOptions(expression, getOptions())));
        return expression.withNode(translation);
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
