/*
 * Copyright 2017-2019 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms.channelmanager.content.documenttype.util;

import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.easymock.EasyMock;
import org.junit.Test;
import org.onehippo.repository.mock.MockNode;

import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class JcrMultipleStringReaderTest {

    @Test
    public void isSingleton() {
        assertTrue(JcrMultipleStringReader.get() == JcrMultipleStringReader.get());
    }

    @Test
    public void readMultipleString() throws RepositoryException {
        final MockNode node = MockNode.root();
        node.setProperty("prop", new String[]{"a", "b"});

        assertThat(JcrMultipleStringReader.get().read(node, "prop").get(), equalTo(new String[]{"a", "b"}));
    }

    @Test
    public void readSingleStringBecomesArray() throws RepositoryException {
        final MockNode node = MockNode.root();
        node.setProperty("prop", "value");

        assertThat(JcrMultipleStringReader.get().read(node, "prop").get(), equalTo(new String[] { "value" }));
    }

    @Test
    public void readMissingProperty() throws RepositoryException {
        final MockNode node = MockNode.root();
        assertThat(JcrMultipleStringReader.get().read(node, "prop"), equalTo(Optional.empty()));
    }

    @Test
    public void readProblem() throws RepositoryException {
        final Node node = EasyMock.createMock(Node.class);
        expect(node.hasProperty(anyString())).andReturn(true);
        expect(node.getProperty("prop")).andThrow(new RepositoryException());
        expect(node.getPath()).andReturn("/");
        replay(node);

        assertThat(JcrMultipleStringReader.get().read(node, "prop"), equalTo(Optional.empty()));
    }
}