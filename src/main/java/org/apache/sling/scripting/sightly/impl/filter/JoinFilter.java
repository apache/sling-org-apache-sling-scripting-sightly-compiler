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

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.apache.sling.scripting.sightly.compiler.expression.Expression;
import org.apache.sling.scripting.sightly.compiler.expression.ExpressionNode;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.RuntimeCall;

/**
 * Filter providing support for the {@code join} option applied to arrays.
 */
public class JoinFilter extends AbstractFilter {

    public static final String JOIN_OPTION = "join";
    private static final Set<String> OPTIONS = Collections.singleton(JOIN_OPTION);

    private static final class JoinFilterLoader {
        private static final JoinFilter INSTANCE = new JoinFilter();
    }

    private JoinFilter() {
        super(NON_PARAMETRIZABLE_CONTEXTS, OPTIONS, OPTIONS);
    }

    public static JoinFilter getInstance() {
        return JoinFilterLoader.INSTANCE;
    }

    @Override
    protected Expression apply(Expression expression, Map<String, ExpressionNode> options) {
        ExpressionNode translation =
                new RuntimeCall(RuntimeCall.JOIN, expression.getRoot(), options.get(JOIN_OPTION));
        return expression.withNode(translation);
    }
}
