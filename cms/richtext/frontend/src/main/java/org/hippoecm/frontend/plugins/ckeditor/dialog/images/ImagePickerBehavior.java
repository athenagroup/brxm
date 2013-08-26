/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.ckeditor.dialog.images;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.ckeditor.dialog.model.CKEditorImage;
import org.hippoecm.frontend.plugins.richtext.AbstractAjaxDialogBehavior;

public class ImagePickerBehavior extends AbstractAjaxDialogBehavior {

    private static final long serialVersionUID = 1L;

    private final CKEditorImageService imageService;
    private final String editorId;

    public ImagePickerBehavior(final IPluginContext context,
                               final IPluginConfig config,
                               final CKEditorImageService imageService,
                               final String editorId) {
        super(context, config);
        this.imageService = imageService;
        this.editorId = editorId;
    }

    @Override
    protected void respond(AjaxRequestTarget target) {
        IModel<CKEditorImage> model = new Model<CKEditorImage>(imageService.createCKEditorImage(getParameters()));
        getDialogService().show(new ImageBrowserDialog(getPluginContext(), getPluginConfig(), model, editorId));
    }

    @Override
    public void detach(final Component component) {
        super.detach(component);
        if (imageService != null) {
            imageService.detach();
        }
    }

}
