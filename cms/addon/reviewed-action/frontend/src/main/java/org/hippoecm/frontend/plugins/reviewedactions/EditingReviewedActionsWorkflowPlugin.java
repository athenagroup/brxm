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
package org.hippoecm.frontend.plugins.reviewedactions;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.PropertyDefinition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.wicket.Session;
import org.apache.wicket.model.StringResourceModel;

import org.hippoecm.addon.workflow.CompatibilityWorkflowPlugin;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.workflow.WorkflowAction;
import org.hippoecm.frontend.plugins.reviewedactions.dialogs.OnCloseDialog;
import org.hippoecm.frontend.service.EditorException;
import org.hippoecm.frontend.service.IBrowseService;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.IEditorFilter;
import org.hippoecm.frontend.service.IEditorManager;
import org.hippoecm.frontend.service.IValidateService;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.reviewedactions.BasicReviewedActionsWorkflow;

public class EditingReviewedActionsWorkflowPlugin extends CompatibilityWorkflowPlugin implements IValidateService {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(EditingReviewedActionsWorkflowPlugin.class);

    private transient boolean validated = false;
    private transient boolean isvalid = true;
    private transient boolean closing = false;

    public EditingReviewedActionsWorkflowPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        if (config.getString(IValidateService.VALIDATE_ID) != null) {
            context.registerService(this, config.getString(IValidateService.VALIDATE_ID));
        } else {
            log.warn("No validator id {} defined", IValidateService.VALIDATE_ID);
        }

        final IEditor editor = context.getService(config.getString("editor.id"), IEditor.class);
        context.registerService(new IEditorFilter() {
            private static final long serialVersionUID = 1L;

            public void postClose(Object object) {
            }

            public Object preClose() {
                if (!closing) {
                    try {
                        OnCloseDialog.Actions actions = new OnCloseDialog.Actions() {

                            public void revert() {
                                execute(new WorkflowAction() {
                                    private static final long serialVersionUID = 1L;

                                    @Override
                                    public boolean validateSession(List<IValidateService> validators) {
                                        return true;
                                    }

                                    @Override
                                    public void prepareSession(JcrNodeModel handleModel) throws RepositoryException {
                                        Node handleNode = handleModel.getNode();
                                        handleNode.refresh(false);
                                        handleNode.getSession().refresh(true);
                                    }

                                    @Override
                                    public void execute(Workflow wf) throws Exception {
                                        BasicReviewedActionsWorkflow workflow = (BasicReviewedActionsWorkflow) wf;
                                        workflow.disposeEditableInstance();
                                    }
                                }, true);
                            }

                            public void save() {
                                execute(new WorkflowAction() {
                                    private static final long serialVersionUID = 1L;

                                    @Override
                                    public void execute(Workflow wf) throws Exception {
                                        BasicReviewedActionsWorkflow workflow = (BasicReviewedActionsWorkflow) wf;
                                        workflow.commitEditableInstance();
                                    }
                                }, true);
                            }

                            public void close() {
                                IEditor editor = context.getService(config.getString("editor.id"), IEditor.class);
                                try {
                                    // prevent reentrancy
                                    closing = true;
                                    editor.close();
                                } catch (EditorException ex) {
                                    log.error(ex.getMessage());
                                } finally {
                                    closing = false;
                                }
                            }
                        };

                        Node node = ((WorkflowDescriptorModel) getModel()).getNode();
                        boolean dirty = node.isModified();
                        if (!dirty) {
                            HippoSession session = ((HippoSession) node.getSession());
                            NodeIterator nodes = session.pendingChanges(node, "nt:base", true);
                            if (nodes.hasNext()) {
                                dirty = true;
                            }
                        }
                        if (dirty) {
                            IDialogService dialogService = context.getService(IDialogService.class.getName(),
                                    IDialogService.class);
                            dialogService.show(new OnCloseDialog(actions, new JcrNodeModel(node), editor));
                        } else {
                            actions.revert();
                            return new Object();
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        log.error(ex.getMessage());
                        showException(ex);
                    }
                    return null;
                } else {
                    return new Object();
                }
            }

        }, context.getReference(editor).getServiceId());

        addWorkflowAction("save", new StringResourceModel("save", this, null, "Save"), new WorkflowAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void execute(Workflow wf) throws Exception {
                BasicReviewedActionsWorkflow workflow = (BasicReviewedActionsWorkflow) wf;
                workflow.commitEditableInstance();

                ((UserSession) Session.get()).getJcrSession().refresh(false);
                
                // get new instance of the workflow, previous one may have invalidated itself
                EditingReviewedActionsWorkflowPlugin.this.getModel().detach();
                WorkflowDescriptor descriptor = (WorkflowDescriptor)(EditingReviewedActionsWorkflowPlugin.this.getModel().getObject());
                ((UserSession) Session.get()).getJcrSession().refresh(true);
                WorkflowManager manager = ((UserSession) Session.get()).getWorkflowManager();
                workflow = (BasicReviewedActionsWorkflow) manager.getWorkflow(descriptor);

                Document draft = workflow.obtainEditableInstance();
                IModelReference ref = context.getService(config.getString("model.id"), IModelReference.class);
                ref.setModel(new JcrNodeModel(((UserSession) Session.get()).getJcrSession().getNodeByUUID(draft.getIdentity())));

            }
        });

        addWorkflowAction("done", new StringResourceModel("done", this, null, "Done"), new WorkflowAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void execute(Workflow wf) throws Exception {
                BasicReviewedActionsWorkflow workflow = (BasicReviewedActionsWorkflow)wf;
                workflow.commitEditableInstance();
                closing = true;
                IEditor editor = context.getService(getPluginConfig().getString(IEditorManager.EDITOR_ID), IEditor.class);
                editor.close();
                System.err.println("BERRY#0 "+config.getString("browser.id"));
                IBrowseService browser = context.getService("browser.id", IBrowseService.class);
                System.err.println("BERRY#1 "+browser);
                System.err.println("BERRY#2 "+((WorkflowDescriptorModel)EditingReviewedActionsWorkflowPlugin.this.getModel()));
                System.err.println("BERRY#3 "+((WorkflowDescriptorModel)EditingReviewedActionsWorkflowPlugin.this.getModel()).getNode());
                browser.browse(new JcrNodeModel(((WorkflowDescriptorModel)EditingReviewedActionsWorkflowPlugin.this.getModel()).getNode()));
            }
        });
    }

    public boolean hasError() {
        if (!validated) {
            validate();
        }
        return !isvalid;
    }

    public void validate() {
        isvalid = true;
        try {
            Node handle = ((WorkflowDescriptorModel) getModel()).getNode();
            String currentUser = handle.getSession().getUserID();
            NodeIterator variants = handle.getNodes(handle.getName());
            Node toBeValidated = null;
            while (variants.hasNext()) {
                Node variant = variants.nextNode();
                if (variant.hasProperty("hippostd:state")
                        && variant.getProperty("hippostd:state").getString().equals("draft")) {
                    if (variant.hasProperty("hippostd:holder")
                            && variant.getProperty("hippostd:holder").getString().equals(currentUser)) {
                        toBeValidated = variant;
                        break;
                    }
                }
            }
            if (toBeValidated != null) {
                PropertyIterator properties = toBeValidated.getProperties();
                while (properties.hasNext()) {
                    PropertyDefinition propertyDefinition = properties.nextProperty().getDefinition();
                    if (propertyDefinition.isMandatory()) {
                        String propName = propertyDefinition.getName();
                        if (toBeValidated.hasProperty(propName)) {
                            Property mandatory = toBeValidated.getProperty(propName);
                            if (mandatory.getDefinition().isMultiple()) {
                                if (mandatory.getLengths().length == 0) {
                                    isvalid = false;
                                    error("Mandatory field " + propName + " has no value.");
                                    break;
                                } else {
                                    for (Value value : mandatory.getValues()) {
                                        if (value.getString() == null || value.getString().equals("")) {
                                            isvalid = false;
                                            error("Mandatory field " + propName + " has no value.");
                                            break;
                                        }
                                    }
                                }
                            } else {
                                Value value = mandatory.getValue();
                                if (value == null || value.getString() == null || value.getString().equals("")) {
                                    isvalid = false;
                                    error("Mandatory field " + propName + " has no value.");
                                    break;
                                }
                            }
                        } else {
                            isvalid = false;
                            error("Mandatory field " + propName + " has no value.");
                            break;
                        }
                    }
                }
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
            error("Problem while validating: " + ex.getMessage());
            isvalid = false;
        }
        validated = true;
    }
}
