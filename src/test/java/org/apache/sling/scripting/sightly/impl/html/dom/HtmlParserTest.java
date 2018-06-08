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

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.scripting.sightly.impl.html.dom.template.Template;
import org.apache.sling.scripting.sightly.impl.html.dom.template.TemplateCommentNode;
import org.apache.sling.scripting.sightly.impl.html.dom.template.TemplateElementNode;
import org.apache.sling.scripting.sightly.impl.html.dom.template.TemplateNode;
import org.apache.sling.scripting.sightly.impl.html.dom.template.TemplateParser;
import org.apache.sling.scripting.sightly.impl.html.dom.template.TemplateTextNode;
import org.junit.Test;
import org.powermock.reflect.Whitebox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class HtmlParserTest {

    private static final String DATA_TEST_ATTRIBUTE = "data-test-attribute";

    /**
     * Pretty print a template nodes structure for debugging of failed tests
     *
     * @param indentation - indentation level (the method is used recursively)
     * @param node - template nodes to print
     */
    private static void print(int indentation, TemplateNode node) {
        if (node == null) {
            return;
        }

        List<TemplateNode> children = null;
        String name = "UNKNOWN";


        if (node.getClass() == Template.class) {
            Template template = (Template)node;
            children = template.getChildren();
            name = template.getName();
        } else if (node.getClass() == TemplateElementNode.class) {
            TemplateElementNode element = (TemplateElementNode)node;
            children = element.getChildren();
            name = "ELEMENT: " + element.getName();
        } else if (node.getClass() == TemplateTextNode.class) {
            name = "TEXT: " + ((TemplateTextNode)node).getText();
        } else if (node.getClass() == TemplateCommentNode.class) {
            name = "COMMENT: " + ((TemplateCommentNode)node).getText();
        }

        System.out.print(StringUtils.repeat("\t", indentation));
        System.out.println(name.replace("\n","\\n").replace("\r", "\\r"));
        if (children == null) {
            return;
        }
        for (TemplateNode child : children) {
            print(indentation + 1, child);
        }
    }

    /**
     * Assert helper to compare two template nodes structures
     *
     * @param reference - reference nodes structure
     * @param parsed - parsed nodes structure
     */
    private static void assertSameStructure(TemplateNode reference, TemplateNode parsed) {
        if (parsed == null || reference == null) {
            assertSame("Expecting both null", parsed, reference);
            return;
        }
        assertEquals("Expecting same class", reference.getClass(), parsed.getClass());

        List<TemplateNode> parsedChildren = null, referenceChildren = null;

        if (parsed.getClass() == Template.class) {
            Template parsedTemplate = (Template)parsed;
            Template referenceTemplate = (Template)reference;
            assertEquals("Expecting same name",
                    referenceTemplate.getName(),
                    parsedTemplate.getName());
            parsedChildren = parsedTemplate.getChildren();
            referenceChildren = referenceTemplate.getChildren();
        } else if (parsed.getClass() == TemplateElementNode.class) {
            TemplateElementNode parsedElement = (TemplateElementNode)parsed;
            TemplateElementNode referenceElement = (TemplateElementNode)reference;
            assertEquals("Expecting same name",
                    referenceElement.getName(),
                    parsedElement.getName());
            parsedChildren = parsedElement.getChildren();
            referenceChildren = referenceElement.getChildren();
        } else if (parsed.getClass() == TemplateTextNode.class) {
            assertEquals("Expecting same content",
                    ((TemplateTextNode)reference).getText(),
                    ((TemplateTextNode)parsed).getText());
        } else if (parsed.getClass() == TemplateCommentNode.class) {
            assertEquals("Expecting same content",
                    ((TemplateCommentNode)reference).getText(),
                    ((TemplateCommentNode)parsed).getText());
        }

        if (parsedChildren == null || referenceChildren == null) {
            assertSame("Expecting both children null", parsedChildren, referenceChildren);
            return;
        }

        assertEquals("Expecting same number of children",
                parsedChildren.size(),
                referenceChildren.size());

        for (int i = 0, n = parsedChildren.size(); i < n; i++) {
            assertSameStructure(parsedChildren.get(i), referenceChildren.get(i));
        }
    }

    /**
     * Create a basic template nodes structure containing one text nodes and one comment nodes
     *
     * @param textAndComment - String containing text (optional) and comment
     * @return a reference template
     */
    private Template createReference(String textAndComment) {
        int commentIx = textAndComment.indexOf("<!");
        if (commentIx < 0 || commentIx > textAndComment.length()) {
            throw new IndexOutOfBoundsException("String must contain text and comment");
        }
        Template reference = new Template();
        if (commentIx > 0) {
            reference.addChild(new TemplateTextNode(
                    textAndComment.substring(0, commentIx)));
        }
        reference.addChild(new TemplateCommentNode(
                textAndComment.substring(commentIx)));
        return reference;
    }

    @Test
    public void testParseCommentSpanningAcrossCharBuffer() throws Exception {
        String[] testStrings = new String[] {
                "<!--/* comment */-->",
                "1<!--/* comment */-->",
                "12<!--/* comment */-->",
                "123<!--/* comment */-->",
                "1234<!--/* comment */-->",
                "12345<!--/* comment */-->",
                "123456<!--/* comment */-->",
                "1234567<!--/* comment */-->",
                "12345678<!--/* comment */-->",
                "123456789<!--/* comment */-->",
                "1234567890<!--/* comment */-->",
                "12345678901<!--/* comment */-->"
        };
        Template reference = null, parsed = null;
        Whitebox.setInternalState(HtmlParser.class, "BUF_SIZE", 10);

        try {
            for (String test : testStrings) {
                StringReader reader = new StringReader(test);
                reference = createReference(test);

                TemplateParser.TemplateParserContext context = new TemplateParser.TemplateParserContext();
                HtmlParser.parse(reader, context);
                parsed = context.getTemplate();

                assertSameStructure(parsed, reference);
            }
        } catch (AssertionError e) {
            System.out.println("Reference: ");
            print(0, reference);
            System.out.println("Parsed: ");
            print(0, parsed);
            throw e;
        }
    }

    @Test
    public void testVoidElements() throws IOException {
        VoidAndSelfClosingElementsDocumentHandler voidAndSelfClosingElementsDocumentHandler = new VoidAndSelfClosingElementsDocumentHandler();
        for (String tag : HtmlParser.VOID_ELEMENTS) {
            StringReader stringReader = new StringReader("<" + tag + " " + DATA_TEST_ATTRIBUTE + ">");
            HtmlParser.parse(stringReader, voidAndSelfClosingElementsDocumentHandler);
            assertEquals("Parsed tag is not what was expected.", tag, voidAndSelfClosingElementsDocumentHandler.getLastProcessedTag());
        }
        assertEquals("Expected 0 invocations for 'onEndElement'", 0, voidAndSelfClosingElementsDocumentHandler.onEndElementInvocations);
        assertEquals("Expected as many invocations for 'onStartElement' as many void elements.", HtmlParser.VOID_ELEMENTS.size(),
                voidAndSelfClosingElementsDocumentHandler.onStartElementInvocations);

        HtmlParser.parse(new StringReader("<img " + DATA_TEST_ATTRIBUTE + "/>"), voidAndSelfClosingElementsDocumentHandler);
        assertEquals("Expected 0 invocations for 'onEndElement'", 0, voidAndSelfClosingElementsDocumentHandler.onEndElementInvocations);
        assertEquals("Expected as many invocations for 'onStartElement' as many void elements + 1.", HtmlParser.VOID_ELEMENTS.size() + 1,
                voidAndSelfClosingElementsDocumentHandler.onStartElementInvocations);
    }

    @Test
    public void testExplicitlyClosedElements() throws IOException {
        TestDocumentHandler handler = new TestDocumentHandler();
        Set<String> tags = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("a", "p", "div")));
        for (String tag : tags) {
            StringReader stringReader = new StringReader("<" + tag + " " + DATA_TEST_ATTRIBUTE + "></" + tag + ">");
            HtmlParser.parse(stringReader, handler);
            assertEquals("Parsed tag is not what was expected.", tag, handler.getLastProcessedTag());
        }
        assertEquals("Expected as many invocations for 'onEndElement' as many test elements.", tags.size(),
                handler.onEndElementInvocations);
        assertEquals("Expected as many invocations for 'onStartElement' as many test elements.", tags.size(),
                handler.onStartElementInvocations);
    }

    abstract class AbstractDocumentHandler implements DocumentHandler {

        private String lastProcessedTag;

        @Override
        public void onCharacters(char[] ch, int off, int len) {

        }

        @Override
        public void onComment(String characters) {

        }

        @Override
        public void onStartElement(String name, AttributeList attList, boolean endSlash) {
            lastProcessedTag = name;
        }

        @Override
        public void onEndElement(String name) {

        }

        @Override
        public void onStart() {

        }

        @Override
        public void onEnd() {

        }

        String getLastProcessedTag() {
            return lastProcessedTag;
        }
    }

    class VoidAndSelfClosingElementsDocumentHandler extends AbstractDocumentHandler {

        int onEndElementInvocations = 0;
        int onStartElementInvocations = 0;

        @Override
        public void onStartElement(String name, AttributeList attList, boolean endSlash) {
            super.onStartElement(name, attList, endSlash);
            assertTrue("Expected a " + DATA_TEST_ATTRIBUTE + ".",
                    attList.attributeCount() == 1 && attList.containsAttribute(DATA_TEST_ATTRIBUTE) &&
                            attList.getValue(DATA_TEST_ATTRIBUTE) == null);
            assertTrue("Expected a self-closing attribute.", endSlash);
            onStartElementInvocations++;
        }

        @Override
        public void onEndElement(String name) {
            super.onEndElement(name);
            onEndElementInvocations++;
        }
    }

    class TestDocumentHandler extends AbstractDocumentHandler {
        int onEndElementInvocations = 0;
        int onStartElementInvocations = 0;

        @Override
        public void onStartElement(String name, AttributeList attList, boolean endSlash) {
            super.onStartElement(name, attList, endSlash);
            assertTrue("Expected a " + DATA_TEST_ATTRIBUTE + ".",
                    attList.attributeCount() == 1 && attList.containsAttribute(DATA_TEST_ATTRIBUTE) &&
                            attList.getValue(DATA_TEST_ATTRIBUTE) == null);
            assertFalse("Did not expect a self-closing attribute.", endSlash);
            onStartElementInvocations++;
        }

        @Override
        public void onEndElement(String name) {
            super.onEndElement(name);
            onEndElementInvocations++;
        }
    }




}
