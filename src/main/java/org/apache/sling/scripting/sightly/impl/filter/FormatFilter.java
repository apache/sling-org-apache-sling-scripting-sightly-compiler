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
    public static final String TIMEZONE_OPTION = "timezone";

    private static final class FormatFilterLoader {
        private static final FormatFilter INSTANCE = new FormatFilter();
    }

    private FormatFilter() {
        super(NON_PARAMETRIZABLE_CONTEXTS, new HashSet<>(Arrays.asList(FORMAT_OPTION, TYPE_OPTION, I18nFilter.LOCALE_OPTION, TIMEZONE_OPTION)), Collections.singleton(FORMAT_OPTION));
    }

    public static FormatFilter getInstance() {
        return FormatFilterLoader.INSTANCE;
    }

    @Override
    protected Expression apply(Expression expression, Map<String, ExpressionNode> options) {
        ExpressionNode translation =
                new RuntimeCall(RuntimeCall.FORMAT, expression.getRoot(), new MapLiteral(options));
        return expression.withNode(translation);
    }
}
