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
 * The {@code URIManipulationFilter} provides support for Sightly's URI Manipulation options according to the
 * <a href="https://github.com/Adobe-Marketing-Cloud/htl-spec/blob/1.2/SPECIFICATION.md">language specification</a>
 */
public class URIManipulationFilter extends AbstractFilter {

    public static final String SCHEME = "scheme";
    public static final String DOMAIN = "domain";
    public static final String PATH = "path";
    public static final String APPEND_PATH = "appendPath";
    public static final String PREPEND_PATH = "prependPath";
    public static final String SELECTORS = "selectors";
    public static final String ADD_SELECTORS = "addSelectors";
    public static final String REMOVE_SELECTORS = "removeSelectors";
    public static final String EXTENSION = "extension";
    public static final String SUFFIX = "suffix";
    public static final String PREPEND_SUFFIX = "prependSuffix";
    public static final String APPEND_SUFFIX = "appendSuffix";
    public static final String FRAGMENT = "fragment";
    public static final String QUERY = "query";
    public static final String ADD_QUERY = "addQuery";
    public static final String REMOVE_QUERY = "removeQuery";

    private static final Set<ExpressionContext> APPLICABLE_CONTEXTS;

    static {
        Set<ExpressionContext> applicableContexts = new HashSet<>(NON_PARAMETRIZABLE_CONTEXTS);
        applicableContexts.remove(ExpressionContext.PLUGIN_DATA_SLY_RESOURCE);
        applicableContexts.remove(ExpressionContext.PLUGIN_DATA_SLY_INCLUDE);
        APPLICABLE_CONTEXTS = Collections.unmodifiableSet(applicableContexts);
    }


    private static final class URIManipulationFilterLoader {
        private static final URIManipulationFilter INSTANCE = new URIManipulationFilter();
    }

    private URIManipulationFilter() {
        super(APPLICABLE_CONTEXTS, new HashSet<>(Arrays.asList(SCHEME, DOMAIN, PATH, APPEND_PATH, PREPEND_PATH, SELECTORS,
                ADD_SELECTORS, REMOVE_SELECTORS, EXTENSION, SUFFIX, PREPEND_SUFFIX, APPEND_SUFFIX, FRAGMENT, QUERY, ADD_QUERY,
                REMOVE_QUERY)), Collections.emptySet());
    }

    public static URIManipulationFilter getInstance() {
        return URIManipulationFilterLoader.INSTANCE;
    }

    @Override
    protected Expression apply(Expression expression, Map<String, ExpressionNode> options) {
        if (options.size() > 0) {
            ExpressionNode translation =
                    new RuntimeCall(RuntimeCall.URI_MANIPULATION, expression.getRoot(), new MapLiteral(options));
            return expression.withNode(translation);
        }
        return expression;
    }
}
