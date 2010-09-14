/*
 *  Copyright 2010 Hippo.
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
package org.hippoecm.repository.translation.impl;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NameFactory;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.hippoecm.repository.FacetedNavigationEngine;
import org.hippoecm.repository.FacetedNavigationEngine.Context;
import org.hippoecm.repository.FacetedNavigationEngine.Query;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.dataprovider.DataProviderContext;
import org.hippoecm.repository.dataprovider.HippoVirtualProvider;
import org.hippoecm.repository.dataprovider.IFilterNodeId;
import org.hippoecm.repository.dataprovider.MirrorNodeId;
import org.hippoecm.repository.dataprovider.StateProviderContext;
import org.hippoecm.repository.dataprovider.ViewNodeId;
import org.hippoecm.repository.translation.HippoTranslationNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TranslationVirtualProvider extends HippoVirtualProvider {

    static final Logger log = LoggerFactory.getLogger(TranslationVirtualProvider.class);

    private HippoVirtualProvider subNodesProvider;
    private FacetedNavigationEngine<Query, Context> facetedEngine;
    private FacetedNavigationEngine.Context facetedContext;

    private Name handleName;
    private Name idName;
    private Name localeName;

    @Override
    protected void initialize() throws RepositoryException {
        NameFactory nf = NameFactoryImpl.getInstance();
        register(nf.create(HippoTranslationNodeType.NS, "translations"), null);
        handleName = resolveName(HippoNodeType.NT_HANDLE);
        idName = nf.create(HippoTranslationNodeType.NS, "id");
        localeName = nf.create(HippoTranslationNodeType.NS, "locale");
    }

    @Override
    public void initialize(DataProviderContext stateMgr) throws RepositoryException {
        super.initialize(stateMgr);
        this.facetedEngine = stateMgr.getFacetedEngine();
        this.facetedContext = stateMgr.getFacetedContext();
    }

    @Override
    public NodeState populate(StateProviderContext context, NodeState state) throws RepositoryException {
        log.debug("populating " + state.getNodeId());
        
        if (subNodesProvider == null) {
            subNodesProvider = getDataProviderContext().lookupProvider(resolveName(HippoNodeType.NT_MIRROR));
            if (subNodesProvider == null) {
                return super.populate(context, state);
            }
        }

        NodeId parentId = state.getParentId();
        NodeId docId = parentId;
        if (docId instanceof MirrorNodeId) {
            docId = ((MirrorNodeId) docId).getCanonicalId();
        }
        String[] ids = getProperty(docId, idName);
        if (ids == null || ids.length == 0) {
            return super.populate(context, state);
        }
        String id = ids[0];
        log.debug("  id=" + id);

        boolean singledView = false;
        LinkedHashMap<Name, String> view = null;
        LinkedHashMap<Name, String> order = null;

        if (parentId instanceof IFilterNodeId) {
            IFilterNodeId filterNodeId = (IFilterNodeId) parentId;
            if (filterNodeId.getView() != null) {
                view = new LinkedHashMap<Name, String>(filterNodeId.getView());
            }
            if (filterNodeId.getOrder() != null) {
                order = new LinkedHashMap<Name, String>(filterNodeId.getOrder());
            }
            singledView = filterNodeId.isSingledView();
        }

        FacetedNavigationEngine.Result facetedResult = facetedEngine.query(
                "//element(*,hippotranslation:translated)[@hippotranslation:id='" + id + "']", facetedContext);
        if (facetedResult.length() > 0) { // NPE if we don't check
            for (NodeId t9nDocId : facetedResult) {
                if (t9nDocId == null)
                    continue;

                NodeState t9nDocState = getCanonicalNodeState(t9nDocId);
                if (t9nDocState == null)
                    continue;

                NodeId t9nParentId = t9nDocState.getParentId();
                if (t9nParentId == null) {
                    continue;
                }
                NodeState grandParentState = getNodeState(t9nParentId, context);
                if (grandParentState.getNodeTypeName().equals(handleName)) {
                    if (view != null && !match(view, t9nDocId)) {
                        continue;
                    }
                }

                boolean found = true;
                String[] languages = getProperty(t9nDocId, localeName);
                NodeId ancestorId = t9nDocId;
                while (languages == null || languages.length != 1) {
                    NodeState ancestorState = getNodeState(ancestorId, context);
                    ancestorId = ancestorState.getParentId();
                    if (ancestorId == null) {
                        found = false;
                        break;
                    }
                    // FIXME: check node of ancestor?
                    languages = getProperty(ancestorId, localeName);
                }
                if (!found) {
                    continue;
                }
                Name name = resolveName(languages[0]);

                state.addChildNodeEntry(name, new ViewNodeId(subNodesProvider, state.getNodeId(), t9nDocId, context,
                        name, view, order, singledView));
            }
        }
        return state;
    }

    // FIXME: copied from ViewVirtualProvider
    protected boolean match(Map<Name, String> view, NodeId candidate) throws RepositoryException {
        for (Map.Entry<Name, String> entry : view.entrySet()) {
            Name facet = entry.getKey();
            String value = entry.getValue();
            String[] matching = getProperty(candidate, facet);
            if (matching != null && matching.length > 0) {
                if (value != null && !value.equals("") && !value.equals("*")) {
                    int i;
                    for (i = 0; i < matching.length; i++) {
                        if (matching[i].equals(value)) {
                            break;
                        }
                    }
                    if (i == matching.length) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

}
