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
package org.hippoecm.frontend.plugins.standardworkflow;

import java.rmi.RemoteException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.Session;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.validation.validator.PatternValidator;
import org.hippoecm.addon.workflow.CompatibilityWorkflowPlugin;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.dialog.IDialogService.Dialog;
import org.hippoecm.frontend.model.JcrItemModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.IEditorManager;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.service.ServiceException;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.widgets.TextFieldWidget;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.standardworkflow.EditmodelWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EditmodelWorkflowPlugin extends CompatibilityWorkflowPlugin {

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(EditmodelWorkflowPlugin.class);

    public EditmodelWorkflowPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);
        add(new WorkflowAction("edit", new StringResourceModel("edit", this, null)) {
            private static final long serialVersionUID = 1L;

            @Override
            public String execute(Workflow workflow) throws Exception {
                EditmodelWorkflow emWorkflow = (EditmodelWorkflow) workflow;
                if (emWorkflow != null) {
                    String path = emWorkflow.edit();
                    try {
                        Node node = ((UserSession) Session.get()).getJcrSession().getRootNode().getNode(
                                path.substring(1));
                        JcrItemModel itemModel = new JcrItemModel(node);
                        if (path != null) {
                            IEditorManager editorMgr = context.getService(config.getString(IEditorManager.EDITOR_ID),
                                    IEditorManager.class);
                            if (editorMgr != null) {
                                JcrNodeModel nodeModel = new JcrNodeModel(itemModel);
                                editorMgr.openEditor(nodeModel);
                            } else {
                                log.warn("No view service found");
                            }
                        } else {
                            log.error("no model found to edit");
                        }
                    } catch (RepositoryException ex) {
                        log.error(ex.getMessage());
                    }
                } else {
                    log.error("no workflow defined on model for selected node");
                }
                return null;
            }
        });

        add(new WorkflowAction("copy", new StringResourceModel("copy", this, null)) {
            private static final long serialVersionUID = 1L;

            String name;

            @Override
            protected Dialog createRequestDialog() {
                return new CopyModelDialog(this);
            }

            @Override
            protected String execute(Workflow wf) {
                try {
                    EditmodelWorkflow workflow = (EditmodelWorkflow) wf;
                    if (workflow != null) {
                        String path = workflow.copy(name);
                        ((UserSession) Session.get()).getJcrSession().refresh(true);

                        JcrNodeModel nodeModel = new JcrNodeModel(new JcrItemModel(path));
                        if (path != null) {
                            IPluginContext context = EditmodelWorkflowPlugin.this.getPluginContext();
                            IPluginConfig config = EditmodelWorkflowPlugin.this.getPluginConfig();

                            IEditorManager editService = context.getService(config.getString(IEditorManager.EDITOR_ID),
                                    IEditorManager.class);
                            IEditor editor = editService.openEditor(nodeModel);
                            IRenderService renderer = context.getService(context.getReference(editor).getServiceId(),
                                    IRenderService.class);
                            if (renderer != null) {
                                renderer.focus(null);
                            }
                        } else {
                            log.error("no model found to edit");
                        }
                    } else {
                        log.error("no workflow defined on model for selected node");
                    }
                } catch (WorkflowException ex) {
                    return ex.getClass().getName() + ": " + ex.getMessage();
                } catch (ServiceException ex) {
                    return ex.getClass().getName() + ": " + ex.getMessage();
                } catch (RemoteException ex) {
                    return ex.getClass().getName() + ": " + ex.getMessage();
                } catch (RepositoryException ex) {
                    return ex.getClass().getName() + ": " + ex.getMessage();
                }
                return null;
            }
        });
    }

    public class CopyModelDialog extends CompatibilityWorkflowPlugin.WorkflowAction.WorkflowDialog {
        private static final long serialVersionUID = 1L;

        public CopyModelDialog(CompatibilityWorkflowPlugin.WorkflowAction action) {
            action.super();
            WorkflowDescriptorModel workflowModel = (WorkflowDescriptorModel) EditmodelWorkflowPlugin.this.getModel();
            PropertyModel model = new PropertyModel(action, "name");
            try {
                model.setObject(workflowModel.getNode().getName());
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
            MarkupContainer widget = new TextFieldWidget("name", model);
            ((FormComponent) widget.get("widget")).add(new PatternValidator("[a-z]+"));
            add(widget);
        }

        @Override
        protected void onOk() {
            super.onOk();
        }

        @Override
        public IModel getTitle() {
            return new StringResourceModel("copy-model", this, null);
        }
    }
}
