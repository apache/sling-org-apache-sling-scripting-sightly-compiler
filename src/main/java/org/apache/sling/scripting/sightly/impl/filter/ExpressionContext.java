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
import java.util.Set;

import org.apache.sling.scripting.sightly.compiler.expression.Expression;

/**
 * Defines a context for the {@link Expression} that will be processed by a {@link Filter}. The context can then be used by filters to
 * further enhance the decision mechanism for their processing.
 */
public enum ExpressionContext {

    // Plugin contexts
    PLUGIN_DATA_SLY_USE(Collections.emptySet()),
    PLUGIN_DATA_SLY_TEXT(Collections.emptySet()),
    PLUGIN_DATA_SLY_ATTRIBUTE(Collections.emptySet()),
    PLUGIN_DATA_SLY_ELEMENT(Collections.emptySet()),
    PLUGIN_DATA_SLY_TEST(Collections.emptySet()),
    PLUGIN_DATA_SLY_SET(Collections.emptySet()),
    PLUGIN_DATA_SLY_LIST(new HashSet<>(Arrays.asList("begin", "step", "end"))),
    PLUGIN_DATA_SLY_REPEAT(new HashSet<>(Arrays.asList("begin", "step", "end"))),
    PLUGIN_DATA_SLY_INCLUDE(new HashSet<>(Arrays.asList("appendPath", "prependPath", "file", "requestAttributes"))),
    PLUGIN_DATA_SLY_RESOURCE(new HashSet<>(Arrays.asList("appendPath", "prependPath", "file", "selectors", "addSelectors",
            "removeSelectors", "resourceType", "requestAttributes"))),
    PLUGIN_DATA_SLY_TEMPLATE(Collections.emptySet()),
    PLUGIN_DATA_SLY_CALL(Collections.emptySet()),
    PLUGIN_DATA_SLY_UNWRAP(Collections.emptySet()),

    // Markup contexts
    ELEMENT(Collections.emptySet()),
    TEXT(Collections.emptySet()),
    ATTRIBUTE(Collections.emptySet());

    private static final String PLUGIN_PREFIX = "PLUGIN_DATA_SLY_";

    private final Set<String> options;

    ExpressionContext(Set<String> options) {
        this.options = options;
    }

    /**
     * Retrieves the context for the plugin specified by {@code pluginName}.
     *
     * @param pluginName the name of the plugin for which to retrieve the context
     * @return the context
     * @throws IllegalArgumentException if the plugin identified by {@code pluginName} doesn't have a context associated
     */
    public static ExpressionContext getContextForPlugin(String pluginName) {
        return valueOf(PLUGIN_PREFIX + pluginName.toUpperCase());
    }

    public Set<String> getOptions() {
        return options;
    }
}
