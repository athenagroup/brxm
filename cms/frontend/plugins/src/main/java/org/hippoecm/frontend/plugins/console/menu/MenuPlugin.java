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
package org.hippoecm.frontend.plugins.console.menu;

import org.apache.wicket.model.Model;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.DialogLink;
import org.hippoecm.frontend.dialog.IDialogFactory;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.console.menu.cnd.CndExportDialog;
import org.hippoecm.frontend.plugins.console.menu.cnd.CndImportDialog;
import org.hippoecm.frontend.plugins.console.menu.content.ContentExportDialog;
import org.hippoecm.frontend.plugins.console.menu.content.ContentImportDialog;
import org.hippoecm.frontend.plugins.console.menu.copy.CopyDialog;
import org.hippoecm.frontend.plugins.console.menu.delete.DeleteDialog;
import org.hippoecm.frontend.plugins.console.menu.move.MoveDialog;
import org.hippoecm.frontend.plugins.console.menu.namespace.NamespaceDialog;
import org.hippoecm.frontend.plugins.console.menu.node.NodeDialog;
import org.hippoecm.frontend.plugins.console.menu.property.PropertyDialog;
import org.hippoecm.frontend.plugins.console.menu.rename.RenameDialog;
import org.hippoecm.frontend.plugins.console.menu.reset.ResetDialog;
import org.hippoecm.frontend.plugins.console.menu.save.SaveDialog;
import org.hippoecm.frontend.plugins.console.menu.sorter.Sorter;
import org.hippoecm.frontend.service.IJcrService;
import org.hippoecm.frontend.service.render.RenderPlugin;

public class MenuPlugin extends RenderPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    private static final long serialVersionUID = 1L;
    
    private Sorter sorter;

    public MenuPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        IDialogService dialogService = getDialogService();

        IDialogFactory dialogFactory = new IDialogFactory() {
            private static final long serialVersionUID = 1L;

            public AbstractDialog createDialog(IDialogService service) {
                return new NodeDialog(MenuPlugin.this, getPluginContext(), service);
            }
        };
        add(new DialogLink("node-dialog", new Model("Add Node"), dialogFactory, dialogService));

        dialogFactory = new IDialogFactory() {
            private static final long serialVersionUID = 1L;

            public AbstractDialog createDialog(IDialogService service) {
                return new DeleteDialog(MenuPlugin.this, getPluginContext(), service);
            }
        };
        add(new DialogLink("delete-dialog", new Model("Delete Node"), dialogFactory, dialogService));

        dialogFactory = new IDialogFactory() {
            private static final long serialVersionUID = 1L;

            public AbstractDialog createDialog(IDialogService service) {
                return new SaveDialog(MenuPlugin.this, getPluginContext(), service);
            }
        };
        add(new DialogLink("save-dialog", new Model("Save"), dialogFactory, dialogService));

        dialogFactory = new IDialogFactory() {
            private static final long serialVersionUID = 1L;

            public AbstractDialog createDialog(IDialogService service) {
                return new ResetDialog(MenuPlugin.this, getPluginContext(), service);
            }
        };
        add(new DialogLink("reset-dialog", new Model("Reset"), dialogFactory, dialogService));

        dialogFactory = new IDialogFactory() {
            private static final long serialVersionUID = 1L;

            public AbstractDialog createDialog(IDialogService service) {
                return new ContentExportDialog(MenuPlugin.this, getPluginContext(), service);
            }
        };
        add(new DialogLink("content-export-dialog", new Model("Export Node"), dialogFactory, dialogService));

        dialogFactory = new IDialogFactory() {
            private static final long serialVersionUID = 1L;

            public AbstractDialog createDialog(IDialogService service) {
                return new ContentImportDialog(MenuPlugin.this, getPluginContext(), service);
            }
        };
        add(new DialogLink("content-import-dialog", new Model("Import Node"), dialogFactory, dialogService));

        
        dialogFactory = new IDialogFactory() {
            private static final long serialVersionUID = 1L;

            public AbstractDialog createDialog(IDialogService service) {
                return new PropertyDialog(MenuPlugin.this, getPluginContext(), service);
            }
        };
        add(new DialogLink("property-dialog", new Model("Add Property"), dialogFactory, dialogService));

        dialogFactory = new IDialogFactory() {
            private static final long serialVersionUID = 1L;

            public AbstractDialog createDialog(IDialogService service) {
                return new RenameDialog(MenuPlugin.this, getPluginContext(), service);
            }
        };
        add(new DialogLink("rename-dialog", new Model("Rename Node"), dialogFactory, dialogService));

        dialogFactory = new IDialogFactory() {
            private static final long serialVersionUID = 1L;

            public AbstractDialog createDialog(IDialogService service) {
                return new MoveDialog(MenuPlugin.this, getPluginContext(), service);
            }
        };
        add(new DialogLink("move-dialog", new Model("Move Node"), dialogFactory, dialogService));

        dialogFactory = new IDialogFactory() {
            private static final long serialVersionUID = 1L;

            public AbstractDialog createDialog(IDialogService service) {
                return new CopyDialog(MenuPlugin.this, getPluginContext(), service);
            }
        };
        add(new DialogLink("copy-dialog", new Model("Copy Node"), dialogFactory, dialogService));
        

        dialogFactory = new IDialogFactory() {
            private static final long serialVersionUID = 1L;

            public AbstractDialog createDialog(IDialogService service) {
                return new CndImportDialog(MenuPlugin.this, getPluginContext(), service);
            }
        };
        add(new DialogLink("cnd-import-dialog", new Model("CND Import"), dialogFactory, dialogService));

        dialogFactory = new IDialogFactory() {
            private static final long serialVersionUID = 1L;

            public AbstractDialog createDialog(IDialogService service) {
                return new CndExportDialog(MenuPlugin.this, getPluginContext(), service);
            }
        };
        add(new DialogLink("cnd-export-dialog", new Model("CND Export"), dialogFactory, dialogService));

        
        dialogFactory = new IDialogFactory() {
            private static final long serialVersionUID = 1L;

            public AbstractDialog createDialog(IDialogService service) {
                return new NamespaceDialog(MenuPlugin.this, getPluginContext(), service);
            }
        };
        add(new DialogLink("namespace-dialog", new Model("Add Namespace"), dialogFactory, dialogService));
        
        sorter = new Sorter("sorter-panel");
        add(sorter);
    }
    
    @Override
    public void onModelChanged() {
        sorter.setModel(getModel());
    }
    
    @Override
    public void redraw() {
        super.redraw();
    }

    public void flushNodeModel(JcrNodeModel nodeModel) {
        IJcrService jcrService = getPluginContext().getService(IJcrService.class.getName(), IJcrService.class);
        if (jcrService != null) {
            jcrService.flush(nodeModel);
        }
    }

}
