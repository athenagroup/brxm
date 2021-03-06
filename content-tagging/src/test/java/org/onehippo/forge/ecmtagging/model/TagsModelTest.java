/*
 *  Copyright 2008-2019 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.forge.ecmtagging.model;

import javax.jcr.Node;
import javax.jcr.Property;

import org.hippoecm.frontend.PluginTest;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.junit.Test;
import org.onehippo.forge.ecmtagging.editor.TagsModel;

import static org.junit.Assert.assertEquals;

public class TagsModelTest extends PluginTest {

    @Test
    public void testAddTags() throws Exception {
        Node node = root.addNode("testEmpty");
        Property prop = node.setProperty("tags", new String[] {});
        TagsModel model = new TagsModel(new JcrPropertyModel(prop));
        assertEquals("", model.getObject());
        model.addTag("tag1");
        assertEquals("tag1, ", model.getObject());
        model.addTag("Tag2");
        assertEquals("tag1, Tag2, ", model.getObject());

        node = root.addNode("testFilled");
        prop = node.setProperty("tags", new String[] {"tag1"});
        model = new TagsModel(new JcrPropertyModel<String>(prop));
        assertEquals("tag1, ", model.getObject());
        model.addTag("Tag2");
        assertEquals("tag1, Tag2, ", model.getObject());
    }

    @Test
    public void testAddTagsLowerCase() throws Exception {
        Node node = root.addNode("testEmpty");
        Property prop = node.setProperty("tags", new String[] {});
        TagsModel model = new TagsModel(new JcrPropertyModel(prop));
        model.setToLowerCase(true);
        model.addTag("TAG1");
        assertEquals("tag1, ", model.getObject());
        model.addTag("TaG2");
        assertEquals("tag1, tag2, ", model.getObject());
    }

}
