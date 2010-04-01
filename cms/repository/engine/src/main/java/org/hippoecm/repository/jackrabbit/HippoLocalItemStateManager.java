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
package org.hippoecm.repository.jackrabbit;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.WeakHashMap;

import javax.jcr.NamespaceException;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.HierarchyManager;
import org.apache.jackrabbit.core.ItemId;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.PropertyId;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.observation.EventStateCollectionFactory;
import org.apache.jackrabbit.core.state.ChangeLog;
import org.apache.jackrabbit.core.state.ForkedXAItemStateManager;
import org.apache.jackrabbit.core.state.ItemState;
import org.apache.jackrabbit.core.state.ItemStateCacheFactory;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.ItemStateManager;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeReferences;
import org.apache.jackrabbit.core.state.NodeReferencesId;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.state.SharedItemStateManager;
import org.apache.jackrabbit.core.state.StaleItemStateException;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.conversion.IllegalNameException;
import org.apache.jackrabbit.spi.commons.conversion.MalformedPathException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hippoecm.repository.FacetedNavigationEngine;
import org.hippoecm.repository.FacetedNavigationEngine.Context;
import org.hippoecm.repository.FacetedNavigationEngine.Query;
import org.hippoecm.repository.Modules;

public class HippoLocalItemStateManager extends ForkedXAItemStateManager implements DataProviderContext {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    protected final Logger log = LoggerFactory.getLogger(HippoLocalItemStateManager.class);

    /** Mask pattern indicating a regular, non-virtual JCR item
     */
    static final int ITEM_TYPE_REGULAR  = 0x00;

    /** Mask pattern indicating an externally defined node, patterns can
     * be OR-ed to indicate both external and virtual nodes.
     */
    static final int ITEM_TYPE_EXTERNAL = 0x01;

    /** Mask pattern indicating a virtual node, patterns can be OR-ed to
     * indicate both external and virtual nodes.
     */
    static final int ITEM_TYPE_VIRTUAL  = 0x02;

    NodeTypeRegistry ntReg;
    protected org.apache.jackrabbit.core.SessionImpl session;
    protected HierarchyManager hierMgr;
    FacetedNavigationEngine<Query, Context> facetedEngine;
    FacetedNavigationEngine.Context facetedContext;
    protected HippoLocalItemStateManager.FilteredChangeLog filteredChangeLog = null;
    protected boolean noUpdateChangeLog = false;
    protected Map<String,HippoVirtualProvider> virtualProviders;
    protected Map<Name,HippoVirtualProvider> virtualNodeNames;
    protected Set<Name> virtualPropertyNames;
    private Set<ItemState> virtualStates = new HashSet<ItemState>();
    private Map<NodeId,ItemState> virtualNodes = new HashMap<NodeId,ItemState>();
    Map<ItemId,Object> deletedExternals = new WeakHashMap<ItemId,Object>();
    private NodeId rootNodeId;
    private boolean virtualLayerEnabled = false;
    private int virtualLayerEnabledCount = 0;
    private boolean virtualLayerRefreshing = true;

    public HippoLocalItemStateManager(SharedItemStateManager sharedStateMgr, EventStateCollectionFactory factory,
                                      ItemStateCacheFactory cacheFactory, String attributeName, NodeTypeRegistry ntReg, boolean enabled,
                                      NodeId rootNodeId) {
        super(sharedStateMgr, factory, attributeName, cacheFactory);
        this.ntReg = ntReg;
        this.virtualLayerEnabled = enabled;
        this.rootNodeId = rootNodeId;
        virtualProviders = new HashMap<String,HippoVirtualProvider>();
        virtualNodeNames = new HashMap<Name,HippoVirtualProvider>();
        virtualPropertyNames = new HashSet<Name>();
    }

    public boolean isEnabled() {
        return virtualLayerEnabled && virtualLayerEnabledCount == 0;
    }
    public void setEnabled(boolean enabled) {
        if(enabled) {
            --virtualLayerEnabledCount;
        } else {
            ++virtualLayerEnabledCount;
        }
    }
    public void setRefreshing(boolean enabled) {
        virtualLayerRefreshing = enabled;
    }
    
    public NodeTypeRegistry getNodeTypeRegistry() {
        return ntReg;
    }

    public HierarchyManager getHierarchyManager() {
        return hierMgr;
    }

    public FacetedNavigationEngine<FacetedNavigationEngine.Query, Context> getFacetedEngine() {
        return facetedEngine;
    }

    public FacetedNavigationEngine.Context getFacetedContext() {
        return facetedContext;
    }

    public void registerProvider(Name nodeTypeName, HippoVirtualProvider provider) {
        virtualNodeNames.put(nodeTypeName, provider);
    }

    public void registerProviderProperty(Name propName) {
        virtualPropertyNames.add(propName);
    }

    public void registerProvider(String moduleName, HippoVirtualProvider provider) {
        virtualProviders.put(moduleName, provider);
    }

    public HippoVirtualProvider lookupProvider(String moduleName) {
        return virtualProviders.get(moduleName);
    }

    public HippoVirtualProvider lookupProvider(Name nodeTypeName) {
        return virtualNodeNames.get(nodeTypeName);
    }
    
    public Name getQName(String name) throws IllegalNameException, NamespaceException {
        return session.getQName(name);
    }
    
    public Path getQPath(String path) throws MalformedPathException, IllegalNameException, NamespaceException {
        return session.getQPath(path);
    }

    void initialize(org.apache.jackrabbit.core.SessionImpl session,
                    FacetedNavigationEngine<Query, Context> facetedEngine,
                    FacetedNavigationEngine.Context facetedContext) {
        this.session = session;
        this.hierMgr = session.getHierarchyManager();
        this.facetedEngine = facetedEngine;
        this.facetedContext = facetedContext;

        LinkedHashSet<DataProviderModule> providerInstances = new LinkedHashSet<DataProviderModule>();
        if (virtualLayerEnabled) {
            Modules<DataProviderModule> modules;
            modules = new Modules<DataProviderModule>(getClass().getClassLoader(), DataProviderModule.class);
            for(DataProviderModule module : modules) {
                log.info("Provider module "+module.toString());
                providerInstances.add(module);
            }
        }

        for(DataProviderModule provider : providerInstances) {
            if(provider instanceof HippoVirtualProvider) {
                registerProvider(provider.getClass().getName(), (HippoVirtualProvider)provider);
            }
        }
        for(DataProviderModule provider : providerInstances) {
            try {
                provider.initialize(this);
            } catch(RepositoryException ex) {
                log.error("cannot initialize virtual provider "+provider.getClass().getName()+": "+ex.getMessage(), ex);
            }
        }
    }

    @Override
    public void dispose() {
        facetedEngine.unprepare(facetedContext);
        super.dispose();
    }

    @Override
    public synchronized void edit() throws IllegalStateException {
        if(inEditMode()) {
            return;
        }
        super.edit();
    }

    @Override
    protected void update(ChangeLog changeLog) throws ReferentialIntegrityException, StaleItemStateException,
                                                      ItemStateException {
        filteredChangeLog = new FilteredChangeLog(changeLog);
        virtualStates.clear();
        virtualNodes.clear();
        filteredChangeLog.invalidate();
        if(!noUpdateChangeLog) {
            super.update(filteredChangeLog);
        }
        deletedExternals.putAll(filteredChangeLog.deletedExternals);
    }

    @Override
    public void update()
    throws ReferentialIntegrityException, StaleItemStateException, ItemStateException, IllegalStateException {
        super.update();
        edit();
        FilteredChangeLog tempChangeLog = filteredChangeLog;
        filteredChangeLog = null;
        if(tempChangeLog != null) {
            tempChangeLog.repopulate();
        }
    }

    void refresh() throws ReferentialIntegrityException, StaleItemStateException, ItemStateException {
        noUpdateChangeLog = true;
        update();
        noUpdateChangeLog = false;
    }

    @Override
    public ItemState getItemState(ItemId id) throws NoSuchItemStateException, ItemStateException {
        ItemState state = super.getItemState(id);
        if(id instanceof HippoNodeId) {
            if(!virtualNodes.containsKey((NodeId)id)) {
                edit();
                NodeState nodeState = (NodeState) state;
                if(isEnabled()) {
                    nodeState = ((HippoNodeId)id).populate((StateProviderContext)null, nodeState);
                } else {
                    // keep nodestate as is
                }

                Name nodeTypeName = nodeState.getNodeTypeName();
                if(virtualNodeNames.containsKey(nodeTypeName) && !virtualStates.contains(state)) {
                    int type =  isVirtual(nodeState);
                    if( (type & ITEM_TYPE_EXTERNAL) != 0  && (type & ITEM_TYPE_VIRTUAL) != 0) {
                        nodeState.removeAllChildNodeEntries();
                    }
                    nodeState = ((HippoNodeId)id).populate(virtualNodeNames.get(nodeTypeName), nodeState);
                }
                virtualNodes.put((HippoNodeId)id, nodeState);
                store(nodeState);
                return nodeState;
            }
        } else if(state instanceof NodeState) {
                NodeState nodeState = (NodeState) state;
                Name nodeTypeName = nodeState.getNodeTypeName();
                if(virtualNodeNames.containsKey(nodeTypeName) && !virtualStates.contains(state)) {
                    edit();
                    int type = isVirtual(nodeState);
                    if ((type & ITEM_TYPE_EXTERNAL) != 0) {
                        nodeState.removeAllChildNodeEntries();
                    }
                    try {
                        virtualStates.add(state);
                        if(id instanceof ArgumentNodeId) {
                            state = virtualNodeNames.get(nodeTypeName).populate(new StateProviderContext(((ArgumentNodeId)id).getArgument()), nodeState);
                        } else if(id instanceof HippoNodeId) {
                            state = ((HippoNodeId)id).populate(virtualNodeNames.get(nodeTypeName), nodeState);
                        } else {
                            state = virtualNodeNames.get(nodeTypeName).populate(new StateProviderContext(), nodeState);
                        }
                        store(state);
                        return nodeState;
                    } catch (RepositoryException ex) {
                        log.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
                        throw new ItemStateException("Failed to populate node state", ex);
                    }
            }
        }
        return state;
    }

    @Override
    public boolean hasItemState(ItemId id) {
            if(id instanceof HippoNodeId) {
                return true;
            }
            return super.hasItemState(id);
    }

    @Override
    public NodeState getNodeState(NodeId id) throws NoSuchItemStateException, ItemStateException {
        NodeState state = null;
        try {
            state = super.getNodeState(id);
        } catch(NoSuchItemStateException ex) {
            if(!(id instanceof HippoNodeId)) {
                throw ex;
            }
        } catch(ItemStateException ex) {
            if(!(id instanceof HippoNodeId)) {
                throw ex;
            }
        }

        if(virtualNodes.containsKey(id)) {
            state = (NodeState) virtualNodes.get(id);
        } else if(state == null && id instanceof HippoNodeId) {
            edit();
            NodeState nodeState;
            if (isEnabled()) {
                nodeState = ((HippoNodeId)id).populate(null);
                if (nodeState == null) {
                    throw new NoSuchItemStateException("Populating node failed");
                }
            } else {
                nodeState = populate((HippoNodeId)id);
            }

            virtualNodes.put((HippoNodeId)id, nodeState);
            store(nodeState);

                Name nodeTypeName = nodeState.getNodeTypeName();
                if(virtualNodeNames.containsKey(nodeTypeName)) {
                    int type = isVirtual(nodeState);
                    /*
                     * If a node is EXTERNAL && VIRTUAL, we are dealing with an already populated nodestate.
                     * Since the parent EXTERNAL node can impose new constaints, like an inherited filter, we
                     * first need to remove all the childNodeEntries, and then populate it again
                     */
                    if( (type  & ITEM_TYPE_EXTERNAL) != 0  && (type  & ITEM_TYPE_VIRTUAL) != 0) {
                        nodeState.removeAllChildNodeEntries();
                    }
                    state = ((HippoNodeId)id).populate(virtualNodeNames.get(nodeTypeName), nodeState);
                }

            return nodeState;
        }
        return state;
    }

    @Override
    public PropertyState getPropertyState(PropertyId id) throws NoSuchItemStateException, ItemStateException {
        return super.getPropertyState(id);
    }
    
    private NodeState populate(HippoNodeId nodeId) throws NoSuchItemStateException, ItemStateException {
        NodeState dereference = getNodeState(rootNodeId);
        NodeState state = createNew(nodeId, dereference.getNodeTypeName(), nodeId.parentId);
        state.setNodeTypeName(dereference.getNodeTypeName());
        state.setDefinitionId(dereference.getDefinitionId());
        return state;
    }

    boolean isPureVirtual(ItemId id) {
        if (id.denotesNode()) {
            if (id instanceof HippoNodeId) {
                return true;
            }
        } else {
            try {
                PropertyState propState = (PropertyState)getItemState(id);
                return (propState.getParentId() instanceof HippoNodeId);
            } catch (NoSuchItemStateException ex) {
                return true;
            } catch (ItemStateException ex) {
                return true;
            }
        }
        return false;
    }

    int isVirtual(ItemState state) {
        if(state.isNode()) {
            int type = ITEM_TYPE_REGULAR;
            if(state.getId() instanceof HippoNodeId) {
                type |= ITEM_TYPE_VIRTUAL;
            }
            if(virtualNodeNames.containsKey(((NodeState)state).getNodeTypeName())) {
                type |= ITEM_TYPE_EXTERNAL;
            }
            return type;
        } else {
            /* it is possible to do a check on type name of the property
             * using Name name = ((PropertyState)state).getName().toString().equals(...)
             * to check and return whether a property is virtual.
             *
             * FIXME: this would be better if these properties would not be
             * named for all node types, but bound to a specific node type
             * for which there is already a provider defined.
             */
            PropertyState propState = (PropertyState) state;
            if(propState.getPropertyId() instanceof HippoPropertyId) {
                return ITEM_TYPE_VIRTUAL;
            } else if(virtualPropertyNames.contains(propState.getName())) {
                return ITEM_TYPE_VIRTUAL;
            } else if(propState.getParentId() instanceof HippoNodeId) {
                return ITEM_TYPE_VIRTUAL;
            } else {
                return ITEM_TYPE_REGULAR;
            }
        }
    }

    class FilteredChangeLog extends ChangeLog {

        private ChangeLog upstream;
        Map<ItemId,Object> deletedExternals = new HashMap<ItemId,Object>();

        FilteredChangeLog(ChangeLog changelog) {
            upstream = changelog;
        }

        void invalidate() {
            if (!virtualLayerRefreshing) {
                return;
            }
            List<ItemState> deletedStates = new LinkedList<ItemState>();
            for(Iterator iter = upstream.deletedStates(); iter.hasNext(); )
                deletedStates.add((ItemState)iter.next());
            List<ItemState> addedStates = new LinkedList<ItemState>();
            for(Iterator iter = upstream.addedStates(); iter.hasNext(); )
                addedStates.add((ItemState)iter.next());
            List<ItemState> modifiedStates = new LinkedList<ItemState>();
            for(Iterator iter = upstream.modifiedStates(); iter.hasNext(); )
                modifiedStates.add((ItemState)iter.next());
            for(ItemState state : deletedStates) {
                if((isVirtual(state) & ITEM_TYPE_EXTERNAL) != 0) {
                    deletedExternals.put(state.getId(), null);
                    ((NodeState)state).removeAllChildNodeEntries();
                    stateDestroyed(state);
                }
            }
            for(ItemState state : addedStates) {
                if((isVirtual(state) & ITEM_TYPE_VIRTUAL) != 0) {
                    if(state.isNode()) {
                        NodeState nodeState = (NodeState) state;
                        try {
                            NodeState parentNodeState = (NodeState) get(nodeState.getParentId());
                            if(parentNodeState != null) {
                                parentNodeState.removeChildNodeEntry(nodeState.getNodeId());
                                stateDestroyed(nodeState);
                            }
                        } catch(NoSuchItemStateException ex) {
                        }
                    } else {
                        stateDestroyed(state);
                    }
                } else if((isVirtual(state) & ITEM_TYPE_EXTERNAL) != 0) {
                    if(!deletedExternals.containsKey(state.getId()) &&
                       !HippoLocalItemStateManager.this.deletedExternals.containsKey(state.getId())) {
                        ((NodeState)state).removeAllChildNodeEntries();
                        stateDestroyed((NodeState)state);
                    }
                }
            }
            for(ItemState state : modifiedStates) {
                if((isVirtual(state) & ITEM_TYPE_EXTERNAL) != 0) {
                    stateDestroyed((NodeState)state);
                    ((NodeState)state).removeAllChildNodeEntries();
                }
            }
        }

        private void repopulate() {
            for(Iterator iter = virtualStates.iterator(); iter.hasNext(); ) {
                ItemState state = (ItemState) iter.next();
                // only repopulate ITEM_TYPE_EXTERNAL, not state that are ITEM_TYPE_EXTERNAL && ITEM_TYPE_VIRTUAL
                if(((isVirtual(state) & ITEM_TYPE_EXTERNAL)) != 0 && ((isVirtual(state) & ITEM_TYPE_VIRTUAL) == 0) &&
                       !deleted(state.getId()) &&
                       !deletedExternals.containsKey(state.getId()) &&
                       !HippoLocalItemStateManager.this.deletedExternals.containsKey(state.getId())) {
                    try {
                        if(state.getId() instanceof ArgumentNodeId) {
                            virtualNodeNames.get(((NodeState)state).getNodeTypeName()).populate(new StateProviderContext(((ArgumentNodeId)state.getId()).getArgument()), (NodeState)state);
                        } else if(state.getId() instanceof HippoNodeId) {
                            ((HippoNodeId)state.getId()).populate(virtualNodeNames.get(((NodeState)state).getNodeTypeName()), (NodeState)state);
                        } else {
                            virtualNodeNames.get(((NodeState)state).getNodeTypeName()).populate(new StateProviderContext(), (NodeState)state);
                        }
                    } catch(ItemStateException ex) {
                        log.error(ex.getClass().getName()+": "+ex.getMessage(), ex);
                    } catch(RepositoryException ex) {
                        log.error(ex.getClass().getName()+": "+ex.getMessage(), ex);
                    }
                }
            }
        }

        @Override
        public void added(ItemState state) {
            upstream.added(state);
        }

        @Override
        public void modified(ItemState state) {
            upstream.modified(state);
        }

        @Override
        public void deleted(ItemState state) {
            upstream.deleted(state);
        }

        @Override
        public void modified(NodeReferences refs) {
            upstream.modified(refs);
        }

        @Override
        public boolean isModified(ItemId id) {
            return upstream.isModified(id);
        }

        @Override public ItemState get(ItemId id) throws NoSuchItemStateException {
            return upstream.get(id);
        }
        @Override public boolean has(ItemId id) {
            return upstream.has(id) && !deletedExternals.containsKey(id);
        }
        @Override public boolean deleted(ItemId id) {
            return upstream.deleted(id) && !deletedExternals.containsKey(id);
        }
        @Override public NodeReferences get(NodeReferencesId id) {
            return upstream.get(id);
        }
        @Override public Iterator addedStates() {
            return new FilteredStateIterator(upstream.addedStates());
        }
        @Override public Iterator modifiedStates() {
            return new FilteredStateIterator(upstream.modifiedStates());
        }
        @Override public Iterator deletedStates() {
            return new FilteredStateIterator(upstream.deletedStates());
        }
        @Override public Iterator modifiedRefs() {
            return new FilteredReferencesIterator(upstream.modifiedRefs());
        }
        @Override public void merge(ChangeLog other) {
            upstream.merge(other);
        }
        @Override public void push() {
            upstream.push();
        }
        @Override public void persisted() {
            upstream.persisted();
        }
        @Override public void reset() {
            upstream.reset();
        }
        @Override public void disconnect() {
            upstream.disconnect();
        }
        @Override public void undo(ItemStateManager parent) {
            upstream.undo(parent);
        }
        @Override public String toString() {
            return upstream.toString();
        }

        class FilteredStateIterator implements Iterator {
            Iterator actualIterator;
            ItemState current;
            FilteredStateIterator(Iterator actualIterator) {
                this.actualIterator = actualIterator;
                current = null;
            }
            public boolean hasNext() {
                while(current == null) {
                    if(!actualIterator.hasNext())
                        return false;
                    current = (ItemState) actualIterator.next();
                    if(needsSkip(current)) {
                        current = null;
                    }
                }
                return (current != null);
            }
            public boolean needsSkip(ItemState current) {
                if (HippoLocalItemStateManager.this.deletedExternals.containsKey(current.getId())) {
                    return true;
                }
                if ((isVirtual(current) & ITEM_TYPE_VIRTUAL) != 0) {
                    return true;
                }
                return false;
            }
            public Object next() throws NoSuchElementException {
                Object rtValue = null;
                while(current == null) {
                    if(!actualIterator.hasNext()) {
                        throw new NoSuchElementException();
                    }
                    current = (ItemState) actualIterator.next();
                    if (needsSkip(current)) {
                        current = null;
                    }
                }
                rtValue = current;
                current = null;
                if(rtValue == null)
                    throw new NoSuchElementException();
                return rtValue;
            }
            public void remove() throws UnsupportedOperationException, IllegalStateException {
                actualIterator.remove();
            }
        }

        class FilteredReferencesIterator implements Iterator {
            Iterator actualIterator;
            NodeReferences current;
            FilteredReferencesIterator(Iterator actualIterator) {
                this.actualIterator = actualIterator;
                current = null;
            }
            public boolean hasNext() {
                while (current == null) {
                    if (!actualIterator.hasNext())
                        return false;
                    current = (NodeReferences)actualIterator.next();
                    if (needsSkip(current)) {
                        current = null;
                    }
                }
                return (current != null);
            }
            public boolean needsSkip(NodeReferences current) {
                return isPureVirtual(current.getTargetId());
            }
            public Object next() throws NoSuchElementException {
                NodeReferences rtValue = null;
                while (current == null) {
                    if (!actualIterator.hasNext()) {
                        throw new NoSuchElementException();
                    }
                    current = (NodeReferences)actualIterator.next();
                    if (needsSkip(current)) {
                        current = null;
                    }
                }
                rtValue = new NodeReferences(current.getId());
                for (PropertyId propId : (List<PropertyId>)current.getReferences()) {
                    if (!isPureVirtual(propId)) {
                        rtValue.addReference(propId);
                    }
                }
                current = null;
                if (rtValue == null)
                    throw new NoSuchElementException();
                return rtValue;
            }
            public void remove() throws UnsupportedOperationException, IllegalStateException {
                actualIterator.remove();
            }
        }
    }

    @Override
    public void stateDestroyed(ItemState destroyed) {
        if(destroyed.getContainer() != this) {
            if ((isVirtual(destroyed) & ITEM_TYPE_EXTERNAL) != 0) {
                deletedExternals.put(destroyed.getId(), null);
            }
        }
        super.stateDestroyed(destroyed);
    }
}
