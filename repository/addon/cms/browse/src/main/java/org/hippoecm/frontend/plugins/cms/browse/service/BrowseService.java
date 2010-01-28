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
package org.hippoecm.frontend.plugins.cms.browse.service;

import java.util.Arrays;
import java.util.List;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.IChangeListener;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.ModelReference;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.cms.browse.model.BrowserSections;
import org.hippoecm.frontend.plugins.cms.browse.model.DocumentCollection;
import org.hippoecm.frontend.plugins.cms.browse.model.DocumentCollectionModel;
import org.hippoecm.frontend.plugins.cms.browse.model.DocumentCollection.DocumentCollectionType;
import org.hippoecm.frontend.service.IBrowseService;
import org.hippoecm.frontend.service.ServiceTracker;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of IBrowseService that also exposes the document model service.
 * <p>
 * The IBrowseService interface should be used by plugins that do not form
 * part of the "browser".  The model services should be used by plugins that do.
 * <p>
 * The document model is always a JcrNodeModel instance, though the
 * nodes may not exist.  When the document node is null, this implies that no
 * document is selected from the folder.  Setting the folder node to null is
 * not supported.
 */
public class BrowseService implements IBrowseService<IModel<Node>>, IDetachable {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    static final Logger log = LoggerFactory.getLogger(BrowseService.class);

    private class DocumentModelService extends ModelReference<Node> {
        private static final long serialVersionUID = 1L;

        DocumentModelService(IPluginConfig config) {
            super(config.getString("model.document"), new JcrNodeModel((Node) null));
        }

        public void updateModel(IModel<Node> model) {
            super.setModel(model);
        }

        @Override
        public void setModel(IModel<Node> model) {
            if (model == null) {
                throw new IllegalArgumentException("invalid model null");
            }
            selectDocument(model, false);
        }
    }

    private DocumentModelService documentService;
    private DocumentCollectionModel collectionModel;
    private BrowserSections sections;

    public BrowseService(final IPluginContext context, final IPluginConfig config, JcrNodeModel document) {
        documentService = new DocumentModelService(config);
        documentService.init(context);

        this.collectionModel = new DocumentCollectionModel(null);
        this.sections = new BrowserSections();
        sections.addListener(new IChangeListener() {
            private static final long serialVersionUID = 1L;

            public void onChange() {
                String active = sections.getActiveSection();
                if (active != null) {
                    IBrowserSection section = sections.getSection(active);
                    collectionModel.setObject(section.getCollection());
                }
            }
        });

        List<String> extensions = Arrays.asList(config.getStringArray("sections"));
        for (final String extension : extensions) {
            context.registerTracker(new ServiceTracker<IBrowserSection>(IBrowserSection.class) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onServiceAdded(IBrowserSection service, String name) {
                    sections.addSection(extension, service);
                    super.onServiceAdded(service, name);
                }

                @Override
                protected void onRemoveService(IBrowserSection service, String name) {
                    super.onRemoveService(service, name);
                    sections.removeSection(extension);
                }

            }, config.getString(extension));
        }
        context.registerService(this, config.getString(IBrowseService.BROWSER_ID, BrowseService.class.getName()));

        browse(document);
    }

    public IModel<DocumentCollection> getCollectionModel() {
        return collectionModel;
    }

    public BrowserSections getSections() {
        return sections;
    }

    /**
     * Use the supplied model of a Node (or Version) to set folder and document models.
     * When a Version is supplied from the version storage, the physical node is used.
     */
    public void browse(IModel<Node> model) {
        selectDocument(model, true);
    }
    
    private void selectDocument(IModel<Node> model, boolean isUpdate) {
        IModel<Node> document = getPhysicalNode(model);
        if (document.getObject() == null) {
            return;
        }
        for (String name : sections.getSections()) {
            IBrowserSection section = sections.getSection(name);
            if (section.contains(document)) {
                section.select(document);
                sections.setActiveSection(name);
                break;
            }
        }
        if (collectionModel.getObject() != null && collectionModel.getObject().getType() == DocumentCollectionType.FOLDER) {
            if (collectionModel.getObject().getFolder().equals(document)) {
                documentService.updateModel(new JcrNodeModel((Node) null));
            } else {
                documentService.updateModel(document);
            }
        } else {
            documentService.updateModel(document);
        }
        onBrowse();
    }

    protected void onBrowse() {
    }

    public void selectSection(IModel<IBrowserSection> model) {
        documentService.updateModel(model.getObject().getCollection().getFolder());
    }

    public void detach() {
        documentService.detach();
    }

    // retrieve the physical node when the node is versioned
    private IModel<Node> getPhysicalNode(IModel<Node> model) {
        Node node = model.getObject();
        if (node != null) {
            try {
                if (node.isNodeType("nt:version")) {
                    Node frozen = node.getNode("jcr:frozenNode");
                    String uuid = frozen.getProperty("jcr:frozenUuid").getString();
                    try {
                        Node docNode = node.getSession().getNodeByUUID(uuid);
                        if (docNode.getDepth() > 0) {
                            Node parent = docNode.getParent();
                            if (parent.isNodeType(HippoNodeType.NT_HANDLE)) {
                                return new JcrNodeModel(parent);
                            }
                        }
                        return new JcrNodeModel(docNode);
                    } catch (ItemNotFoundException infe) {
                        // node doesn't exist anymore.  If it's a document, the handle
                        // should still be available though.
                        if (frozen.hasProperty(HippoNodeType.HIPPO_PATHS)) {
                            Value[] ancestors = frozen.getProperty(HippoNodeType.HIPPO_PATHS).getValues();
                            if (ancestors.length > 1) {
                                uuid = ancestors[1].getString();
                                return new JcrNodeModel(node.getSession().getNodeByUUID(uuid));
                            }
                        }
                        throw infe;
                    }
                }
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
        }
        return model;
    }

}
