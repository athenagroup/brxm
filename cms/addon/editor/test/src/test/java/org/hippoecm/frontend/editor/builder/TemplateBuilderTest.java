/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.frontend.editor.builder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;

import org.hippoecm.frontend.PluginTest;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.plugin.config.ClusterConfigEvent;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.types.IFieldDescriptor;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.hippoecm.frontend.types.JavaFieldDescriptor;
import org.hippoecm.frontend.types.TypeDescriptorEvent;
import org.junit.Test;

public class TemplateBuilderTest extends PluginTest {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id: ";

    @Test
    /**
     * verify that a plugin is added to the cluster when a field is added to the type
     */
    public void testAddField() throws Exception {
        TemplateBuilder builder = new TemplateBuilder("test:test", false, context);

        final List<IPluginConfig> added = new LinkedList<IPluginConfig>();
        final List<IPluginConfig> removed = new LinkedList<IPluginConfig>();
        final List<IPluginConfig> changed = new LinkedList<IPluginConfig>();
        final IClusterConfig config = builder.getTemplate();
        context.registerService(new IObserver() {
            private static final long serialVersionUID = 1L;

            public IObservable getObservable() {
                return config;
            }

            public void onEvent(Iterator<? extends IEvent> events) {
                while (events.hasNext()) {
                    IEvent event = events.next();
                    if (event instanceof ClusterConfigEvent) {
                        ClusterConfigEvent cce = (ClusterConfigEvent) event;
                        IPluginConfig config = cce.getPlugin();
                        switch (cce.getType()) {
                        case PLUGIN_ADDED:
                            added.add(config);
                            break;
                        case PLUGIN_CHANGED:
                            changed.add(config);
                            break;
                        case PLUGIN_REMOVED:
                            removed.add(config);
                            break;
                        }
                    }
                }
            }
            
        }, IObserver.class.getName());

        ITypeDescriptor type = builder.getTypeDescriptor();
        type.addField(new JavaFieldDescriptor("test", "nt:unstructured"));

        home.processEvents();

        assertEquals(1, added.size());
    }

    @Test
    /**
     * verify that a field is removed from the type when a plugin is removed from the cluster
     */
    public void testRemovePlugin() throws Exception {
        TemplateBuilder builder = new TemplateBuilder("test:test", false, context);

        // initialize type descriptor and template
        final ITypeDescriptor type = builder.getTypeDescriptor();
        IClusterConfig config = builder.getTemplate();
        List<IPluginConfig> plugins = config.getPlugins();
        session.save();
        home.processEvents();

        // setup field removal detection
        final List<String> added = new LinkedList<String>();
        final List<String> removed = new LinkedList<String>();
        context.registerService(new IObserver() {

            public IObservable getObservable() {
                return type;
            }

            public void onEvent(Iterator<? extends IEvent> events) {
                while (events.hasNext()) {
                    IEvent event = events.next();
                    if (event instanceof TypeDescriptorEvent) {
                        TypeDescriptorEvent cce = (TypeDescriptorEvent) event;
                        String field = cce.getField().getName();
                        switch (cce.getType()) {
                        case FIELD_ADDED:
                            added.add(field);
                            break;
                        case FIELD_REMOVED:
                            removed.add(field);
                            break;
                        }
                    }
                }
            }
            
        }, IObserver.class.getName());

        // remove a field plugin
        plugins.remove(1);

        home.processEvents();

        assertEquals(1, removed.size());
    }

    @Test
    public void testChangePath() throws Exception {
        TemplateBuilder builder = new TemplateBuilder("test:test", false, context);

        // initialize type descriptor and template
        ITypeDescriptor type = builder.getTypeDescriptor();
        session.save();
        home.processEvents();

        Map<String, IFieldDescriptor> fields = type.getFields();
        IFieldDescriptor titleField = fields.get("title");
        assertEquals("test:title", titleField.getPath());

        JcrNodeModel prototype = builder.getPrototype();
        Node node = prototype.getNode();
        node.setProperty("test:title", "titel");
        
        titleField.setPath("test:titel_new");

        home.processEvents();

        assertTrue(prototype.getNode().hasProperty("test:titel_new"));
        assertEquals("titel", prototype.getNode().getProperty("test:titel_new").getString());
    }

    @Test
    public void testReorderPlugins() throws Exception {
    }

    @Test
    public void testChangeCaption() throws Exception {
    }

}
