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
package org.hippoecm.frontend.plugins.console.dialog;

import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.tree.ITreeState;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.tree.IJcrTreeNode;
import org.hippoecm.frontend.model.tree.JcrTreeModel;
import org.hippoecm.frontend.widgets.JcrTree;

class LookupTargetTreeView extends JcrTree {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private IJcrTreeNode selectedNode;
    private LookupDialog dialog;

    LookupTargetTreeView(String id, JcrTreeModel treeModel, LookupDialog dialog) {
        super(id, treeModel);
        this.dialog = dialog;
    }

    @Override
    protected void onNodeLinkClicked(AjaxRequestTarget target, TreeNode treeNode) {
        if (treeNode instanceof IJcrTreeNode) {
            IJcrTreeNode jcrTreeNode = (IJcrTreeNode) treeNode;
            this.selectedNode = jcrTreeNode;
            dialog.setModel(jcrTreeNode.getNodeModel());
        }
    }

    IJcrTreeNode getSelectedNode() {
        return selectedNode;
    }

    void setSelectedNode(JcrNodeModel selectedNode, JcrTreeModel treeModel) {
        ITreeState treeState = getTreeState();
        TreePath treePath = treeModel.lookup(selectedNode);
        if (treePath != null) {
            for (TreeNode component : (TreeNode[]) treePath.getPath()) {
                treeState.expandNode(component);
            }
    
            TreeNode treeNode = (TreeNode) treePath.getLastPathComponent();
            treeState.selectNode((TreeNode) treePath.getLastPathComponent(), true);
            if (treeNode instanceof IJcrTreeNode) {
                this.selectedNode = (IJcrTreeNode) treeNode;
            }
        }
    }
}
