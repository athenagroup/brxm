/*
 *  Copyright 2008-2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.console.editor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.ConstraintViolationException;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.EmptyDataProvider;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.form.PostOnlyForm;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.properties.JcrPropertiesProvider;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.hippoecm.frontend.plugins.console.behavior.OriginTitleBehavior;
import org.hippoecm.frontend.plugins.console.icons.IconLabel;
import org.hippoecm.frontend.plugins.console.icons.JcrNodeIcon;
import org.hippoecm.frontend.widgets.TextFieldWidget;
import org.onehippo.cm.ConfigurationService;
import org.onehippo.cm.model.ConfigurationModel;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class NodeEditor extends PostOnlyForm<Node> {

    static final Logger log = LoggerFactory.getLogger(NodeEditor.class);

    private static final NamespacePropertyComparator PROPERTY_COMPARATOR = new NamespacePropertyComparator();

    static final String NONE_LABEL = "<none>";

    @SuppressWarnings("unused FieldCanBeLocal")
    private String name;
    @SuppressWarnings("unused FieldCanBeLocal")
    private String uuid;
    @SuppressWarnings("unused FieldCanBeLocal")
    private String nodePath;
    @SuppressWarnings("unused FieldCanBeLocal")
    private String category;
    @SuppressWarnings("unused FieldCanBeLocal")
    private String origin;
    @SuppressWarnings("unused FieldCanBeLocal")
    private String primaryTypeOrigin;
    @SuppressWarnings("unused FieldCanBeLocal")
    private String mixinTypesOrigin;

    private final NamespaceProvider namespaceProvider;
    private final NamespacePropertiesEditor namespacePropertiesEditor;
    private final NodeTypesEditor typesEditor;

    // the (transient, not serializable) HCM ConfigurationService, which is repo-static, but not the model, which can be updated
    private transient ConfigurationService cfgService;

    NodeEditor(final String id, final IModel<Node> model) {
        super(id, model);
        setOutputMarkupId(true);

        add(new ToggleHeader("toggle-header-0", "0", "General"));

        add(new IconLabel("nodeIcon", new LoadableDetachableModel<String>() {
            @Override
            protected String load() {
                return JcrNodeIcon.getIconCssClass(getModelObject());
            }
        }));
        add(new Label("nodePath", new PropertyModel<String>(this, "nodePath")));
        add(new Label("name", new PropertyModel<String>(this, "name")));
        add(new Label("uuid", new PropertyModel<String>(this, "uuid")));

        add(new ToggleHeader("toggle-header-1", "1", "Types"));
        final TextFieldWidget primaryTypeWidget = new TextFieldWidget("primarytype", new PropertyModel<>(this, "primaryType"));
        primaryTypeWidget.setSize("40");
        add(primaryTypeWidget);

        typesEditor = new NodeTypesEditor("mixintypes", model);
        add(typesEditor);
        add(new Label("types", new PropertyModel<String>(typesEditor, "mixinTypes")));

        add(new ToggleHeader("toggle-header-2", "2", "Properties"));
        namespaceProvider = new NamespaceProvider(new EmptyDataProvider<>());
        namespacePropertiesEditor = new NamespacePropertiesEditor("namespaces", namespaceProvider);
        add(namespacePropertiesEditor);

        add(new ToggleHeader("toggle-header-3", "3", "Mixin Types"));

        // HCM config-tracing info
        // TODO: upgrade this to a component with AjaxLinks to the baseline for each origin source file
        add(new MultiLineLabel("origin", new PropertyModel<String>(this, "origin")));

        add(new Label("primarytypeorigin", "")
                .add(new OriginTitleBehavior(new PropertyModel<>(this, "primaryTypeOrigin"))));
        add(new Label("mixintypesorigin", "")
                .add(new OriginTitleBehavior(new PropertyModel<>(this, "mixinTypesOrigin"))));

        onModelChanged();
    }

    @Override
    protected void onDetach() {
        super.onDetach();
        cfgService = null;
    }

    private ConfigurationService getConfigurationService() {
        if (cfgService == null) {
            cfgService = HippoServiceRegistry.getService(ConfigurationService.class);
        }
        return cfgService;
    }

    @Override
    public void onModelChanged() {
        super.onModelChanged();
        final IModel<Node> model = getModel();
        if (model != null && model.getObject() != null) {
            final Node node = model.getObject();
            try {
                namespaceProvider.setWrapped(new JcrPropertiesProvider(new JcrNodeModel(node)));

                nodePath = node.getPath();
                name = node.getName();
                uuid = node.getIdentifier();

                typesEditor.setModel(getModel());
                typesEditor.setVisible(true);

                namespacePropertiesEditor.setVisible(true);

                // update HCM category & origin(s)
                final ConfigurationModel cfgModel = getConfigurationService().getRuntimeConfigurationModel();
                origin = PropertiesEditor.getNodeOrigin(nodePath, cfgModel);
                primaryTypeOrigin = PropertiesEditor.getPropertyOrigin((nodePath.equals("/")? "": nodePath) + "/jcr:primaryType", cfgModel);
                mixinTypesOrigin = PropertiesEditor.getPropertyOrigin((nodePath.equals("/")? "": nodePath) + "/jcr:mixinTypes", cfgModel);

            } catch (final RepositoryException e) {
                log.error(e.getMessage());
            }
        } else {
            // if there is no node, set values to non-null empty strings for safety
            typesEditor.setVisible(false);
            namespacePropertiesEditor.setVisible(false);

            origin = "";
            primaryTypeOrigin = "";
            mixinTypesOrigin = "";
        }
    }

    @SuppressWarnings("unused")
    public String getPrimaryType() {
        final Node node = getModelObject();
        if (node != null) {
            try {
                return node.getPrimaryNodeType().getName();
            } catch (final RepositoryException e) {
                log.error(e.getClass().getName() + ": " + e.getMessage(), e);
            }
        }
        return null;
    }

    @SuppressWarnings("unused")
    public void setPrimaryType(final String primaryType) {
        final Node node = getModelObject();
        if (node != null) {
            String oldPrimaryType = null;
            try {
                oldPrimaryType = node.getPrimaryNodeType().getName();
                node.setPrimaryType(primaryType);
            } catch (final ConstraintViolationException e) {
                log.error("Cannot set primary type to {}", primaryType);
                if (oldPrimaryType != null) {
                    try {
                        node.setPrimaryType(oldPrimaryType);
                    } catch (final RepositoryException e1) {
                        log.error("Failed to set primary node type to previous value");
                    }
                }
            } catch (final RepositoryException e) {
                log.error("Failed to set primary node type", e);
            }
        }
    }

    private static class NamespacePropertiesEditor extends DataView<NamespacePropertiesProvider> {

        NamespacePropertiesEditor(final String id, final IDataProvider<NamespacePropertiesProvider> dataProvider) {
            super(id, dataProvider);
        }

        @Override
        protected void populateItem(final Item<NamespacePropertiesProvider> item) {
            final NamespacePropertiesProvider propertiesProvider = item.getModelObject();

            final String namespace = propertiesProvider.getNamespace();

            final String namespaceHeading = namespace + " (" + propertiesProvider.size() + ")";
            final ToggleHeader toggleHeader = new ToggleHeader("toggle-namespace", namespace, namespaceHeading);
            toggleHeader.setMarkupId("toggle-header-" + namespace);
            toggleHeader.setOutputMarkupId(true);
            item.add(toggleHeader);

            final WebMarkupContainer propertiesContainer = new WebMarkupContainer("propertiesContainer");
            propertiesContainer.setOutputMarkupId(true);
            propertiesContainer.setMarkupId("toggle-box-" + namespace);

            final PropertiesEditor propertiesEditor = new PropertiesEditor("properties", propertiesProvider);
            propertiesContainer.add(propertiesEditor);
            item.add(propertiesContainer);
        }
    }

    private static class NamespaceProvider implements IDataProvider<NamespacePropertiesProvider> {

        private List<NamespacePropertiesProvider> namespaces;
        private IDataProvider<Property> wrapped;

        NamespaceProvider(final IDataProvider<Property> wrapped) {
            this.wrapped = wrapped;
            namespaces = Collections.emptyList();
        }

        void setWrapped(final IDataProvider<Property> wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public Iterator<? extends NamespacePropertiesProvider> iterator(final long first, final long count) {
            load();
            return namespaces.subList((int) first, (int) (first + count)).iterator();
        }

        @Override
        public long size() {
            load();
            return namespaces.size();
        }

        @Override
        public IModel<NamespacePropertiesProvider> model(final NamespacePropertiesProvider object) {
            return Model.of(object);
        }

        @Override
        public void detach() {
            namespaces = null;
            wrapped.detach();
        }

        private void load() {
            namespaces = new ArrayList<>();

            final Map<String, NamespacePropertiesProvider> namespaceMap = new TreeMap<>();
            try {
                final Iterator<? extends Property> it = wrapped.iterator(0, wrapped.size());
                while (it.hasNext()) {
                    final Property p = it.next();
                    final String propName = p.getName();
                    if (!propName.equals("jcr:primaryType") && !propName.equals("jcr:mixinTypes")) {
                        String propNamespace = new JcrName(propName).getNamespace();
                        if (propNamespace == null) {
                            propNamespace = NONE_LABEL;
                        }
                        NamespacePropertiesProvider propertiesProvider = namespaceMap.get(propNamespace);
                        if (propertiesProvider == null) {
                            propertiesProvider = new NamespacePropertiesProvider(propNamespace);
                            namespaceMap.put(propNamespace, propertiesProvider);
                        }

                        propertiesProvider.addProperty(p);
                    }
                }
                namespaces.addAll(namespaceMap.values());
            } catch (final RepositoryException ex) {
                log.error(ex.getMessage());
            }
        }
    }

    protected static class NamespacePropertiesProvider implements IDataProvider<Property> {

        private final String namespace;
        private List<Property> properties;
        private boolean sorted;

        NamespacePropertiesProvider(final String namespace) {
            this.namespace = namespace;
            properties = new ArrayList<>();
            sorted = true;
        }

        public String getNamespace() {
            return namespace;
        }

        void addProperty(final Property p) {
            properties.add(p);
            if (properties.size() > 1) {
                sorted = false;
            }
        }

        @Override
        public Iterator<Property> iterator(final long first, final long count) {
            if (!sorted) {
                properties.sort(PROPERTY_COMPARATOR);
                sorted = true;
            }
            return properties.subList((int) first, (int) (first + count)).iterator();
        }

        @Override
        public long size() {
            return properties.size();
        }

        @Override
        @SuppressWarnings("unchecked")
        public IModel<Property> model(final Property object) {
            return new JcrPropertyModel(object);
        }

        @Override
        public void detach() {
            properties = null;
        }
    }

    private static class NamespacePropertyComparator implements Comparator<Property> {

        public int compare(final Property p1, final Property p2) {
            try {
                if (p1 == null) {
                    if (p2 == null) {
                        return 0;
                    }
                    return 1;
                } else if (p2 == null) {
                    return -1;
                }
                return p1.getName().compareTo(p2.getName());
            } catch (final RepositoryException e) {
                throw new IllegalStateException("Error while comparing properties", e);
            }
        }
    }

}
