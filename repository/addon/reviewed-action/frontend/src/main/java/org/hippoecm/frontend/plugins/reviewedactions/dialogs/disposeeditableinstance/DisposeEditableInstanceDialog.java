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
package org.hippoecm.frontend.plugins.reviewedactions.dialogs.disposeeditableinstance;

import org.hippoecm.frontend.dialog.AbstractWorkflowDialog;
import org.hippoecm.frontend.dialog.DialogWindow;
import org.hippoecm.frontend.plugin.channel.Channel;
import org.hippoecm.repository.reviewedactions.BasicReviewedActionsWorkflow;

public class DisposeEditableInstanceDialog extends AbstractWorkflowDialog {
    private static final long serialVersionUID = 1L;

    public DisposeEditableInstanceDialog(DialogWindow dialogWindow, Channel channel) {
        super(dialogWindow, channel);
        dialogWindow.setTitle("Dispose editable instance");
        if (dialogWindow.getNodeModel().getNode() == null) {
            ok.setVisible(false);
        }
    }

    @Override
    protected void doOk() throws Exception {
        BasicReviewedActionsWorkflow workflow = (BasicReviewedActionsWorkflow) getWorkflow();
        workflow.disposeEditableInstance();
    }

    @Override
    public void cancel() {
    }

}
