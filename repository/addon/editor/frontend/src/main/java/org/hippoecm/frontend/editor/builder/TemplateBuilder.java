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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.collections.MiniMap;
import org.hippoecm.editor.tools.JcrPrototypeStore;
import org.hippoecm.editor.tools.JcrTypeStore;
import org.hippoecm.frontend.editor.impl.BuiltinTemplateStore;
import org.hippoecm.frontend.editor.impl.JcrTemplateStore;
import org.hippoecm.frontend.editor.plugins.field.NodeFieldPlugin;
import org.hippoecm.frontend.editor.plugins.field.PropertyFieldPlugin;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.EventCollection;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObservationContext;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.model.ocm.IStore;
import org.hippoecm.frontend.model.ocm.StoreException;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.ClusterConfigEvent;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.types.BuiltinTypeStore;
import org.hippoecm.frontend.types.IFieldDescriptor;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.hippoecm.frontend.types.JavaFieldDescriptor;
import org.hippoecm.frontend.types.TypeDescriptorEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TemplateBuilder implements IDetachable, IObservable {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(TemplateBuilder.class);

    private String type;
    private boolean readonly;
    private IPluginContext context;
    private IModel selectedExtPtModel;

    private IStore<IClusterConfig> jcrTemplateStore;
    private IStore<IClusterConfig> builtinTemplateStore;

    private IStore<ITypeDescriptor> jcrTypeStore;
    private IStore<ITypeDescriptor> builtinTypeStore;
    private IStore<ITypeDescriptor> fieldTypeStore;

    private JcrPrototypeStore prototypeStore;

    private ITypeDescriptor typeDescriptor;
    private IClusterConfig clusterConfig;
    private JcrNodeModel prototype;
    private Map<String, String> paths;
    private transient Map<String, IFieldDescriptor> removedFields;
    private Map<String, IPluginConfig> pluginCache;

    private IObservationContext obContext;
    private IObserver typeObserver;
    private IObserver clusterObserver;

    public TemplateBuilder(String type, boolean readonly, IPluginContext context, IModel extPtModel)
            throws BuilderException {
        this.type = type;
        this.readonly = readonly;
        this.context = context;
        this.selectedExtPtModel = extPtModel;

        this.jcrTypeStore = new JcrTypeStore();
        this.builtinTypeStore = new BuiltinTypeStore();
        this.fieldTypeStore = new IStore<ITypeDescriptor>() {
            private static final long serialVersionUID = 1L;

            public void close() {
            }

            public void delete(ITypeDescriptor object) throws StoreException {
                throw new UnsupportedOperationException("Read-only store does not support deleting types");
            }

            public Iterator<ITypeDescriptor> find(Map<String, Object> criteria) {
                return jcrTypeStore.find(criteria);
            }

            public ITypeDescriptor load(String id) throws StoreException {
                try {
                    return jcrTypeStore.load(id);
                } catch (StoreException ex) {
                    return builtinTypeStore.load(id);
                }
            }

            public String save(ITypeDescriptor object) throws StoreException {
                throw new UnsupportedOperationException("Read-only store does not support saving types");
            }

        };

        this.jcrTemplateStore = new JcrTemplateStore(fieldTypeStore);
        this.builtinTemplateStore = new BuiltinTemplateStore(fieldTypeStore);

        this.prototypeStore = new JcrPrototypeStore();

        try {
            typeDescriptor = jcrTypeStore.load(type);

            // load template
            @SuppressWarnings("unchecked")
            Map<String, Object> criteria = new MiniMap(1);
            criteria.put("type", typeDescriptor);
            Iterator<IClusterConfig> iter = jcrTemplateStore.find(criteria);
            if (iter.hasNext()) {
                clusterConfig = iter.next();
                initPluginCache();
            } else {
                if (!readonly) {
                    iter = builtinTemplateStore.find(criteria);
                    if (iter.hasNext()) {
                        try {
                            String id = jcrTemplateStore.save(iter.next());
                            clusterConfig = jcrTemplateStore.load(id);
                            initPluginCache();
                        } catch (StoreException ex) {
                            throw new BuilderException("Failed to save generated template", ex);
                        }
                    }
                } else {
                    throw new BuilderException("No template found to display type");
                }
            }
        } catch (StoreException ex) {
            if (!readonly) {
                try {
                    ITypeDescriptor builtin = builtinTypeStore.load(type);
                    String id = jcrTypeStore.save(builtin);
                    if (!id.equals(type)) {
                        throw new BuilderException("Created type descriptor has invalid id " + id);
                    }
                    typeDescriptor = jcrTypeStore.load(id);
                } catch (StoreException ex2) {
                    throw new BuilderException("Could not convert builtin type descriptor to editable copy", ex2);
                }
            } else {
                throw new BuilderException("Could not load type descriptor", ex);
            }
        }

        registerObservers();
    }

    public void dispose() {
        unregisterObservers();
    }

    public String getName() {
        return type;
    }

    public ITypeDescriptor getTypeDescriptor() throws BuilderException {
        return typeDescriptor;
    }

    public IClusterConfig getTemplate() throws BuilderException {
        return clusterConfig;
    }

    public JcrNodeModel getPrototype() throws BuilderException {
        if (prototype == null) {
            prototype = prototypeStore.getPrototype(type, true);
            if (prototype == null) {
                if (readonly) {
                    prototype = prototypeStore.getPrototype(type, false);
                    if (prototype == null) {
                        throw new BuilderException("No prototype found");
                    }
                } else {
                    prototype = prototypeStore.createPrototype(type, true);
                    if (prototype == null) {
                        throw new BuilderException("No prototype could be created for " + type);
                    }
                }
            }
            paths = new HashMap<String, String>();
            for (Map.Entry<String, IFieldDescriptor> entry : getTypeDescriptor().getFields().entrySet()) {
                paths.put(entry.getKey(), entry.getValue().getPath());
            }
        }
        return prototype;
    }

    public void save() throws BuilderException {
        if (readonly) {
            throw new UnsupportedOperationException("readonly builder cannot save artifacts");
        }
        try {
            ITypeDescriptor type = getTypeDescriptor();
            jcrTypeStore.save(type);

            IClusterConfig template = getTemplate();
            jcrTemplateStore.save(template);

            JcrNodeModel nodeModel = getPrototype();
            try {
                nodeModel.getNode().save();
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
        } catch (StoreException ex) {
            log.error(ex.getMessage());
        }
    }

    private void updatePrototype() {
        try {
            ITypeDescriptor typeModel = getTypeDescriptor();
            IModel prototypeModel = getPrototype();

            Node prototype = (Node) prototypeModel.getObject();
            if (prototype != null && typeModel != null) {
                Map<String, String> oldFields = paths;
                paths = new HashMap<String, String>();
                for (Map.Entry<String, IFieldDescriptor> entry : typeModel.getFields().entrySet()) {
                    paths.put(entry.getKey(), entry.getValue().getPath());
                }

                for (Map.Entry<String, String> entry : oldFields.entrySet()) {
                    String oldPath = entry.getValue();
                    IFieldDescriptor newField = typeModel.getField(entry.getKey());
                    if (newField != null) {
                        ITypeDescriptor fieldType = fieldTypeStore.load(newField.getType());
                        if (!newField.getPath().equals(oldPath) && !newField.getPath().equals("*")
                                && !oldPath.equals("*")) {
                            if (fieldType.isNode()) {
                                if (prototype.hasNode(oldPath)) {
                                    Node child = prototype.getNode(oldPath);
                                    child.getSession().move(child.getPath(),
                                            prototype.getPath() + "/" + newField.getPath());
                                }
                            } else {
                                if (prototype.hasProperty(oldPath)) {
                                    Property property = prototype.getProperty(oldPath);
                                    if (property.getDefinition().isMultiple()) {
                                        Value[] values = property.getValues();
                                        property.remove();
                                        if (newField.isMultiple()) {
                                            prototype.setProperty(newField.getPath(), values);
                                        } else if (values.length > 0) {
                                            prototype.setProperty(newField.getPath(), values[0]);
                                        }
                                    } else {
                                        Value value = property.getValue();
                                        property.remove();
                                        if (newField.isMultiple()) {
                                            prototype.setProperty(newField.getPath(), new Value[] { value });
                                        } else {
                                            prototype.setProperty(newField.getPath(), value);
                                        }
                                    }
                                }
                            }
                        } else if (oldPath.equals("*") || newField.getPath().equals("*")) {
                            log.warn("Wildcard fields are not supported");
                        }
                    } else {
                        if (oldPath.equals("*")) {
                            log
                                    .warn("Removing wildcard fields is unsupported.  Items that fall under the definition will not be removed.");
                        } else {
                            if (prototype.hasNode(oldPath)) {
                                prototype.getNode(oldPath).remove();
                            }
                            if (prototype.hasProperty(oldPath)) {
                                prototype.getProperty(oldPath).remove();
                            }
                        }
                    }
                }
            }
        } catch (RepositoryException ex) {
            log.error("Failed to update prototype", ex.getMessage());
        } catch (BuilderException ex) {
            log.error("Incomplete model", ex);
        } catch (StoreException ex) {
            log.error("Invalid field type", ex);
        }
    }

    protected void processFieldAdded(IFieldDescriptor fieldDescriptor) {
        IClusterConfig template;
        try {
            template = getTemplate();
        } catch (BuilderException ex) {
            log.error("Could not find template", ex);
            return;
        }

        ITypeDescriptor fieldType;
        try {
            fieldType = fieldTypeStore.load(fieldDescriptor.getType());
        } catch (StoreException ex) {
            log.error("Unknown type " + fieldDescriptor.getType(), ex);
            return;
        }

        String pluginName = UUID.randomUUID().toString();
        JavaPluginConfig pluginConfig = new JavaPluginConfig(pluginName);
        if (fieldType.isNode()) {
            pluginConfig.put("plugin.class", NodeFieldPlugin.class.getName());
        } else {
            pluginConfig.put("plugin.class", PropertyFieldPlugin.class.getName());
        }
        pluginConfig.put("wicket.id", getSelectedExtensionPoint());
        pluginConfig.put("wicket.model", "${wicket.model}");
        pluginConfig.put("mode", "${mode}");
        pluginConfig.put("engine", "${engine}");
        pluginConfig.put("field", fieldDescriptor.getName());
        pluginConfig.put("caption", fieldType.getName());

        template.getPlugins().add(pluginConfig);
    }

    private void registerObservers() {
        if (typeDescriptor != null) {
            context.registerService(typeObserver = new IObserver() {

                public IObservable getObservable() {
                    return typeDescriptor;
                }

                public void onEvent(Iterator<? extends IEvent> events) {
                    while (events.hasNext()) {
                        IEvent event = events.next();
                        if (event instanceof TypeDescriptorEvent) {
                            TypeDescriptorEvent tde = (TypeDescriptorEvent) event;
                            switch (tde.getType()) {
                            case FIELD_ADDED:
                                processFieldAdded(tde.getField());
                                break;
                            case FIELD_REMOVED:
                            case FIELD_CHANGED:
                            }
                            updatePrototype();
                        }
                    }
                }

            }, IObserver.class.getName());
        }

        if (clusterConfig != null) {
            // maintain a list of field plugins, so that we can clean up when one
            // of them disappears.
            // TODO: check plugin class to verify that the plugin is a fieldplugin
            final Map<String, String> fields = new TreeMap<String, String>();
            for (IPluginConfig plugin : clusterConfig.getPlugins()) {
                if (plugin.getString("field") != null) {
                    fields.put(plugin.getName(), plugin.getString("field"));
                }
            }
            context.registerService(clusterObserver = new IObserver() {

                public IObservable getObservable() {
                    return clusterConfig;
                }

                public void onEvent(Iterator<? extends IEvent> events) {
                    while (events.hasNext()) {
                        IEvent event = events.next();
                        if (event instanceof ClusterConfigEvent) {
                            ClusterConfigEvent cce = (ClusterConfigEvent) event;
                            IPluginConfig config = cce.getPlugin();
                            switch (cce.getType()) {
                            case PLUGIN_ADDED:
                                if (removedFields != null && config.getString("field") != null) {
                                    String field = config.getString("field");
                                    IFieldDescriptor descriptor = removedFields.remove(field);
                                    if (descriptor != null) {
                                        typeDescriptor.addField(descriptor);
                                    }
                                    fields.put(config.getName(), field);
                                }
                                notifyObservers();
                                break;
                            case PLUGIN_CHANGED:
                                // notify observers when the wicket hierarchy has changed
                                if (updatePluginCache()) {
                                    notifyObservers();
                                }
                                break;
                            case PLUGIN_REMOVED:
                                if (fields.containsKey(config.getName())) {
                                    String field = fields.remove(config.getName());
                                    if (removedFields == null) {
                                        removedFields = new TreeMap<String, IFieldDescriptor>();
                                    }
                                    removedFields.put(field, new JavaFieldDescriptor(typeDescriptor.getField(field)));
                                    typeDescriptor.removeField(field);
                                }
                                notifyObservers();
                                break;
                            }
                        }
                    }
                }

            }, IObserver.class.getName());
        }

    }

    private void unregisterObservers() {
        if (typeObserver != null) {
            context.unregisterService(typeObserver, IObserver.class.getName());
            typeObserver = null;
        }
        if (clusterObserver != null) {
            context.unregisterService(clusterObserver, IObserver.class.getName());
            clusterObserver = null;
        }
    }

    private void initPluginCache() {
        this.pluginCache = new TreeMap<String, IPluginConfig>();
        List<IPluginConfig> plugins = clusterConfig.getPlugins();
        for (IPluginConfig plugin : plugins) {
            JavaPluginConfig cache = new JavaPluginConfig();
            cache.put("wicket.id", plugin.get("wicket.id"));
            pluginCache.put(plugin.getName(), cache);
        }
    }

    private boolean updatePluginCache() {
        boolean changed = false;
        List<IPluginConfig> plugins = clusterConfig.getPlugins();
        for (IPluginConfig plugin : plugins) {
            IPluginConfig cache = pluginCache.get(plugin.getName());
            if (cache == null) {
                changed = true;
                break;
            }
            if (cache.getString("wicket.id") != null
                    && !cache.getString("wicket.id").equals(plugin.getString("wicket.id"))) {
                changed = true;
                break;
            }
        }
        if (changed) {
            initPluginCache();
            return true;
        }
        return false;
    }

    // IDetachable

    public void detach() {
        prototypeStore.detach();
    }

    // IObservable

    public void setObservationContext(IObservationContext context) {
        this.obContext = context;
    }

    public void startObservation() {
    }

    public void stopObservation() {
    }

    private void notifyObservers() {
        if (obContext != null) {
            EventCollection<IEvent> collection = new EventCollection<IEvent>();
            collection.add(new IEvent() {

                public IObservable getSource() {
                    return TemplateBuilder.this;
                }

            });
            obContext.notifyObservers(collection);
        }
    }

    private String getSelectedExtensionPoint() {
        return (String) selectedExtPtModel.getObject();
    }

}
