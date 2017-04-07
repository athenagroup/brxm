/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.onehippo.cms7.services.processor.html;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.services.processor.html.filter.Element;
import org.onehippo.cms7.services.processor.html.visit.Tag;
import org.onehippo.cms7.services.processor.html.visit.TagVisitor;
import org.onehippo.repository.mock.MockNode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class HtmlProcessorTest {

    private HtmlProcessor processor;
    private MockNode rootNode;
    private MockNode documentNode;

    // private IModel documentNodeModel;


    @Before
    public void setUp() throws RepositoryException {
        rootNode = MockNode.root();

        documentNode = new MockNode("document");
        rootNode.addNode(documentNode);

//        documentNodeModel = EasyMock.createMock(IModel.class);
//        expect(documentNodeModel.getObject()).andStubReturn(documentNode);
//        replay(documentNodeModel);
    }

    private void assertNoChanges(final String text) throws RepositoryException, IOException {
        assertEquals("Stored text should be returned without changes", emptyIfNull(text), processor.read(text,
                                                                                                         null));

        assertEquals("Text should be stored without changes", emptyIfNull(text), processor.write(text, null));
        assertEquals("Number of child facet nodes should not have changed", documentNode.getNodes().getSize(), documentNode.getNodes().getSize());
    }

    private String emptyIfNull(final String text) {
        return text != null ? text : StringUtils.EMPTY;
    }

    @Test
    public void nullTextDoesNotChange() throws Exception {
        final HtmlProcessorConfig htmlProcessorConfig = new HtmlProcessorConfig();
        processor = new HtmlProcessorImpl(htmlProcessorConfig);

        assertNoChanges(null);
    }

    @Test
    public void emptyTextDoesNotChange() throws Exception {
        final HtmlProcessorConfig htmlProcessorConfig = new HtmlProcessorConfig();
        processor = new HtmlProcessorImpl(htmlProcessorConfig);

        assertNoChanges("");
    }

    @Test
    public void testReadVisitor() throws Exception {
        final HtmlProcessorConfig htmlProcessorConfig = new HtmlProcessorConfig();
        processor = new HtmlProcessorImpl(htmlProcessorConfig);

        TagNameCollector one = new TagNameCollector();
        TagNameCollector two= new TagNameCollector();

        List<TagVisitor> readVisitors = Arrays.asList(one, two);
        String html = processor.read("<h1>Heading 1</h1><h2>Heading 2</h2>", readVisitors);
        assertEquals("<h1>Heading 1</h1><h2>Heading 2</h2>", html);

        List<String> tagsOne = one.getTags();
        assertEquals(2, tagsOne.size());
        assertThat(tagsOne, CoreMatchers.hasItems("h1", "h2"));

        List<String> tagsTwo = two.getTags();
        assertEquals(2, tagsTwo.size());
        assertThat(tagsTwo, CoreMatchers.hasItems("h1", "h2"));
    }

    @Test
    public void testTraversingAfterFiltering() throws Exception {
        final HtmlProcessorConfig htmlProcessorConfig = new HtmlProcessorConfig();
        htmlProcessorConfig.setFilter(true);
        htmlProcessorConfig.setWhitelistElements(Arrays.asList(Element.create("h1"), Element.create("h2")));
        processor = new HtmlProcessorImpl(htmlProcessorConfig);

        TagNameCollector one = new TagNameCollector();

        List<TagVisitor> writeVisitors = Collections.singletonList(one);
        String html = processor.write("<h1>Heading 1</h1><h2>Heading 2</h2><script>alert(\"xss\")</script>", writeVisitors);
        assertEquals("<h1>Heading 1</h1><h2>Heading 2</h2>", html);

        List<String> tagsOne = one.getTags();
        assertEquals(2, tagsOne.size());
        assertThat(tagsOne, CoreMatchers.hasItems("h1", "h2"));
    }

    private static class TagNameCollector implements TagVisitor {

        private List<String> tags = new ArrayList<>();

        public List<String> getTags() {
            return tags;
        }

        @Override
        public void visitBeforeRead(final Tag parent, final Tag tag) throws RepositoryException {
            if (tag != null) {
                String name = tag.getName();
                if (name != null) {
                    tags.add(name);
                }
            }
        }

        @Override
        public void visitBeforeWrite(final Tag parent, final Tag tag) throws RepositoryException {
            if (tag != null) {
                String name = tag.getName();
                if (name != null) {
                    tags.add(name);
                }
            }
        }

        @Override
        public void release() {
        }
    }

}
