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
package org.apache.sling.scripting.sightly.impl.html.dom;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.scripting.sightly.compiler.SightlyCompilerException;
import org.apache.sling.scripting.sightly.impl.html.dom.template.Template;
import org.apache.sling.scripting.sightly.impl.html.dom.template.TemplateAttribute;
import org.apache.sling.scripting.sightly.impl.html.dom.template.TemplateCommentNode;
import org.apache.sling.scripting.sightly.impl.html.dom.template.TemplateElementNode;
import org.apache.sling.scripting.sightly.impl.html.dom.template.TemplateNode;
import org.apache.sling.scripting.sightly.impl.html.dom.template.TemplateTextNode;

public class TreeTraverser {

    private final MarkupHandler handler;

    public TreeTraverser(MarkupHandler handler) {
        this.handler = handler;
    }

    public void traverse(Template template) {
        traverseNode(template);
        this.handler.onDocumentFinished();
    }

    private void traverseNode(TemplateNode node) {
        if (node instanceof TemplateElementNode) {
            traverseElement((TemplateElementNode) node);
        } else if (node instanceof TemplateTextNode) {
            traverseText((TemplateTextNode) node);
        } else if (node instanceof TemplateCommentNode) {
            traverseComment((TemplateCommentNode) node);
        } else {
            throw new IllegalArgumentException("Unknown node type");
        }
    }

    private void traverseElement(TemplateElementNode elem) {
        SightlyCompilerException bubbledError = null;
        StringBuilder offendingInput = new StringBuilder();
        if ("ROOT".equalsIgnoreCase(elem.getName())) {
            traverseChildren(elem);
            return;
        }
        String tagName = elem.getName();

        if (elem.isStartElement()) {
            handler.onOpenTagStart("<" + tagName, tagName);
            try {
                for (TemplateAttribute attribute : elem.getAttributes()) {
                    handler.onAttribute(attribute.getName(), attribute.getValue(), attribute.getQuoteChar());
                }
            } catch (SightlyCompilerException e) {
                if (StringUtils.isEmpty(e.getOffendingInput())) {
                    bubbledError = e;
                    offendingInput.append("<").append(tagName);
                    for (TemplateAttribute attribute : elem.getAttributes()) {
                        String quoteChar = String.valueOf(attribute.getQuoteChar());
                        offendingInput.append(" ").append(attribute.getName());
                        if (StringUtils.isNotEmpty(attribute.getValue())) {
                            offendingInput.append("=").append(quoteChar).append(attribute.getValue()).append(quoteChar);
                        }
                    }
                } else {
                    throw e;
                }
            }
            if (elem.hasEndSlash()) {
                handler.onOpenTagEnd("/>");
                if (bubbledError != null) {
                    offendingInput.append("/>");
                }
            } else {
                handler.onOpenTagEnd(">");
                if (bubbledError != null) {
                    offendingInput.append(">");
                }
            }
            if (bubbledError != null) {
                throw new SightlyCompilerException(bubbledError.getMessage(), offendingInput.toString());
            }
        } else {
            handler.onOpenTagStart("", tagName);
            handler.onOpenTagEnd("");
        }

        traverseChildren(elem);

        if (elem.isHasEndElement()) {
            handler.onCloseTag("</" + elem.getName() + ">");
        } else {
            handler.onCloseTag("");
        }
    }

    private void traverseText(TemplateTextNode textNode) {
        handler.onText(textNode.getText());
    }

    private void traverseComment(TemplateCommentNode comment) {
        handler.onComment(comment.getText());
    }

    private void traverseChildren(TemplateElementNode elem) {
        for (TemplateNode node : elem.getChildren()) {
            traverseNode(node);
        }
    }

}
