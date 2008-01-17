/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.model;

import javax.jcr.Item;

import org.apache.wicket.model.IChainingModel;
import org.apache.wicket.model.IModel;

public abstract class ItemModelWrapper implements IChainingModel {

    protected JcrItemModel itemModel;

    public ItemModelWrapper(JcrItemModel model) {
        itemModel = model;
    }

    public ItemModelWrapper(Item item) {
        itemModel = new JcrItemModel(item);
    }

    public ItemModelWrapper(String path) {
        itemModel = new JcrItemModel(path);
    }

    public JcrItemModel getItemModel() {
        return itemModel;
    }

    // Implement IChainingModel

    public IModel getChainedModel() {
        return itemModel;
    }

    public void setChainedModel(IModel model) {
        if (model instanceof JcrItemModel) {
            itemModel = (JcrItemModel) model;
        }
    }

    public Object getObject() {
        return itemModel.getObject();
    }

    public void setObject(Object object) {
        itemModel.setObject(object);
    }

    public void detach() {
        itemModel.detach();
    }
}
