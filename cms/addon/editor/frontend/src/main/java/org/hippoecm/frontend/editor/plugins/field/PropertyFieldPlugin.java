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
package org.hippoecm.frontend.editor.plugins.field;

import java.util.Iterator;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.editor.ITemplateEngine;
import org.hippoecm.frontend.editor.model.AbstractProvider;
import org.hippoecm.frontend.editor.model.ValueTemplateProvider;
import org.hippoecm.frontend.model.JcrItemModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.types.IFieldDescriptor;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PropertyFieldPlugin extends FieldPlugin<JcrNodeModel, JcrPropertyValueModel> {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(PropertyFieldPlugin.class);

    private JcrNodeModel nodeModel;
    private JcrPropertyModel propertyModel;
    private int nrValues;
    private IObserver propertyObserver;

    public PropertyFieldPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        nodeModel = (JcrNodeModel) getModel();

        // use caption for backwards compatibility; i18n should use field name
        IModel nameModel;
        if (config.getString("caption") != null) {
            nameModel = new Model(config.getString("caption"));
        } else {
            nameModel = new StringResourceModel(fieldName, this, null);
        }
        add(new Label("name", nameModel));

        Label required = new Label("required", "*");
        if (field != null) {
            subscribe();
            if (!field.isMandatory()) {
                required.setVisible(false);
            }
        }
        add(required);

        add(createAddLink());

        updateProvider();
    }

    protected void subscribe() {
        if (!field.getPath().equals("*")) {
            JcrItemModel itemModel = new JcrItemModel(((JcrNodeModel) getModel()).getItemModel().getPath() + "/"
                    + field.getPath());
            propertyModel = new JcrPropertyModel(itemModel);
            nrValues = propertyModel.size();
            getPluginContext().registerService(propertyObserver = new IObserver() {
                private static final long serialVersionUID = 1L;

                public IObservable getObservable() {
                    return propertyModel;
                }

                public void onEvent(Iterator<? extends IEvent> events) {
                    if (propertyModel.size() != nrValues) { //Only redraw if number of properties has changed.
                        nrValues = propertyModel.size();
                        updateProvider();
                        redraw();
                    }
                }

            }, IObserver.class.getName());
        }
    }

    protected void unsubscribe() {
        if (!field.getPath().equals("*")) {
            getPluginContext().unregisterService(propertyObserver, IObserver.class.getName());
            propertyModel = null;
        }
    }

    @Override
    protected AbstractProvider<JcrPropertyValueModel> newProvider(IFieldDescriptor descriptor, ITypeDescriptor type,
            JcrNodeModel nodeModel) {
        if (!descriptor.getPath().equals("*")) {
            ValueTemplateProvider provider = new ValueTemplateProvider(descriptor, type, propertyModel.getItemModel());
            if (ITemplateEngine.EDIT_MODE.equals(mode) && !descriptor.isMultiple() && provider.size() == 0) {
                provider.addNew();
            }
            return provider;
        }
        return null;
    }

    @Override
    public void onModelChanged() {
        // filter out changes in the node model itself.
        // The property model observation takes care of that.
        if (!nodeModel.equals(getModel())) {
            if (field != null) {
                unsubscribe();
                subscribe();
            }
            replace(createAddLink());
            redraw();
        }
    }

    @Override
    protected void onDetach() {
        if (propertyModel != null) {
            propertyModel.detach();
        }
        super.onDetach();
    }

    @Override
    protected void onAddRenderService(Item item, IRenderService renderer) {
        final JcrPropertyValueModel model = findModel(renderer);

        MarkupContainer remove = new AjaxLink("remove") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                onRemoveItem(model, target);
            }
        };
        if (!ITemplateEngine.EDIT_MODE.equals(mode) || (field == null) || field.isMandatory() || !field.isMultiple()) {
            remove.setVisible(false);
        }
        item.add(remove);

        MarkupContainer upLink = new AjaxLink("up") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                onMoveItemUp(model, target);
            }
        };
        if (!ITemplateEngine.EDIT_MODE.equals(mode) || (field == null) || !field.isMultiple() || !field.isOrdered()
                || model.getIndex() == 0) {
            upLink.setVisible(false);
        }
        if (item.getIndex() == 0) {
            upLink.setEnabled(false);
        }
        item.add(upLink);
    }

    // privates

    protected Component createAddLink() {
        if (ITemplateEngine.EDIT_MODE.equals(mode) && (field != null) && field.isMultiple()) {
            return new AjaxLink("add") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    PropertyFieldPlugin.this.onAddItem(target);
                }
            };
        } else {
            return new Label("add").setVisible(false);
        }
    }
}
