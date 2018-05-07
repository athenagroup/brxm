/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.documentworkflow.task;

import java.rmi.RemoteException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionManager;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.repository.documentworkflow.DocumentVariant;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_MIXIN_BRANCH_INFO;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PROPERTY_BRANCH_ID;
import static org.onehippo.repository.documentworkflow.DocumentVariant.CORE_BRANCH_ID;

public class CheckoutBranchTask extends AbstractDocumentTask {

    private static final long serialVersionUID = 1L;

    private DocumentVariant variant;
    private String branchId;

    public DocumentVariant getVariant() {
        return variant;
    }

    public void setVariant(DocumentVariant variant) {
        this.variant = variant;
    }

    public void setBranchId(final String branchId) {
        this.branchId = branchId;
    }

    @Override
    protected Object doExecute() throws WorkflowException, RepositoryException, RemoteException {
        if (getVariant() == null || !getVariant().hasNode()) {
            throw new WorkflowException("No variant provided");
        }
        if (branchId == null) {
            throw new WorkflowException("branchId needs to be provided");
        }

        final Session workflowSession = getWorkflowContext().getInternalWorkflowSession();
        Node targetNode = getVariant().getNode(workflowSession);

        if (targetNode.isNodeType(HIPPO_MIXIN_BRANCH_INFO)) {
            if (branchId.equals(targetNode.getProperty(HIPPO_PROPERTY_BRANCH_ID).getString())) {
                // preview is already for branchId. Nothing to do
                return new Document(targetNode);
            }
        }
        if (branchId.equals(CORE_BRANCH_ID) && !targetNode.isNodeType(HIPPO_MIXIN_BRANCH_INFO)) {
            // preview already core. Nothing to do
            return new Document(targetNode);
        }

        final VersionManager versionManager = workflowSession.getWorkspace().getVersionManager();
        final VersionHistory versionHistory = versionManager.getVersionHistory(targetNode.getPath());

        final String versionLabelToRestore = branchId + "-preview";

        if (!versionHistory.hasVersionLabel(versionLabelToRestore)) {
            throw new WorkflowException(String.format("version label '%s' does not exist in version history so cannot " +
                    "checkout branch '%s'", versionLabelToRestore, branchId));
        }

        // restore the version to preview
        versionManager.restore(versionHistory.getVersionByLabel(versionLabelToRestore), false);
        // after restore, make sure the preview gets checked out
        JcrUtils.ensureIsCheckedOut(targetNode);
        return new Document(targetNode);
    }

}
