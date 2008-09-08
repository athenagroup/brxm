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
package org.hippoecm.frontend.dialog;

import org.apache.wicket.IClusterable;
import org.apache.wicket.Page;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow.PageCreator;

public class DialogAction implements IClusterable {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    private static final long serialVersionUID = 1L;
    
    final private PageCreator pageCreator;
    final private IDialogService dialogService;
    private boolean enabled = true;

    public DialogAction(final IDialogFactory dialogFactory, final IDialogService dialogService) {
        this.dialogService = dialogService;
        pageCreator = new PageCreator() {
            private static final long serialVersionUID = 1L;

            public Page createPage() {
                return dialogFactory.createDialog(dialogService);
            }
        };
    }

    public void execute() {
        dialogService.show(pageCreator.createPage());
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
}
