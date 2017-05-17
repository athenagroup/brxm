/*
 *  Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.impl.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.onehippo.cm.api.model.ConfigurationItemCategory;
import org.onehippo.cm.api.model.ConfigurationNode;
import org.onehippo.cm.api.model.ConfigurationProperty;
import org.onehippo.cm.engine.SnsUtils;

public class ConfigurationNodeImpl extends ConfigurationItemImpl implements ConfigurationNode {

    // Nodes names must always be indexed names, e.g. node[1]
    private final Map<String, ConfigurationNodeImpl> modifiableNodes = new LinkedHashMap<>();
    private final Map<String, ConfigurationNodeImpl> unmodifiableMapWithModifiableNodes = Collections.unmodifiableMap(modifiableNodes);
    private final Map<String, ConfigurationNode> unmodifiableNodes = Collections.unmodifiableMap(modifiableNodes);

    private final Map<String, ConfigurationPropertyImpl> modifiableProperties = new LinkedHashMap<>();
    private final Map<String, ConfigurationPropertyImpl> unmodifiableMapWithModifiableProperties = Collections.unmodifiableMap(modifiableProperties);
    private final Map<String, ConfigurationProperty> unmodifiableProperties = Collections.unmodifiableMap(modifiableProperties);

    private Boolean ignoreReorderedChildren;

    @Override
    public Map<String, ConfigurationNode> getNodes() {
        return unmodifiableNodes;
    }

    public Map<String, ConfigurationNodeImpl> getModifiableNodes() {
        return unmodifiableMapWithModifiableNodes;
    }

    public void addNode(final String name, final ConfigurationNodeImpl node) {
        modifiableNodes.put(name, node);
    }

    public void orderBefore(final String srcChildName, final String destChildName) {
        if (!modifiableNodes.containsKey(srcChildName)) {
            final String msg = String.format("Node '%s' has no child named '%s'.", getPath(), srcChildName);
            throw new IllegalArgumentException(msg);
        }
        if (!modifiableNodes.containsKey(destChildName)) {
            final String msg = String.format("Node '%s' has no child named '%s'.", getPath(), destChildName);
            throw new IllegalArgumentException(msg);
        }

        final List<String> toBeReinsertedChildren = new ArrayList<>();
        boolean destFound = false;

        for (String childName : modifiableNodes.keySet()) {
            if (childName.equals(destChildName)) {
                destFound = true;
            }
            if (destFound && !childName.equals(srcChildName)) {
                toBeReinsertedChildren.add(childName);
            }
        }

        modifiableNodes.put(srcChildName, modifiableNodes.remove(srcChildName));
        toBeReinsertedChildren.forEach(child -> modifiableNodes.put(child, modifiableNodes.remove(child)));

        if (SnsUtils.hasSns(srcChildName, modifiableNodes.keySet())) {
            updateSnsIndices(srcChildName);
        }
    }

    private void updateSnsIndices(final String indexedName) {
        final String unindexedName = SnsUtils.getUnindexedName(indexedName);
        final Map<String, ConfigurationNodeImpl> copy = new LinkedHashMap<>(modifiableNodes);
        modifiableNodes.clear();
        int index = 1;
        for (String sibling : copy.keySet()) {
            if (unindexedName.equals(SnsUtils.getUnindexedName(sibling))) {
                final String newName = SnsUtils.createIndexedName(unindexedName, index);
                final ConfigurationNodeImpl siblingNode = copy.get(sibling);
                siblingNode.setName(newName);
                modifiableNodes.put(newName, copy.get(sibling));
                index++;
            } else {
                modifiableNodes.put(sibling, copy.get(sibling));
            }
        }
    }

    public void removeNode(final String name, boolean updateSnsIndices) {
        modifiableNodes.remove(name);
        if (updateSnsIndices) {
            updateSnsIndices(name);
        }
    }

    public void clearNodes() {
        modifiableNodes.clear();
    }

    public boolean isNew() {
        return modifiableNodes.isEmpty() && modifiableProperties.isEmpty();
    }

    @Override
    public Map<String, ConfigurationProperty> getProperties() {
        return unmodifiableProperties;
    }

    public Map<String, ConfigurationPropertyImpl> getModifiableProperties() {
        return unmodifiableMapWithModifiableProperties;
    }

    public void addProperty(final String name, final ConfigurationPropertyImpl property) {
        modifiableProperties.put(name, property);
    }

    public void removeProperty(final String name) {
        modifiableProperties.remove(name);
    }

    public void clearProperties() {
        modifiableProperties.clear();
    }

    @Override
    public Boolean getIgnoreReorderedChildren() {
        return ignoreReorderedChildren;
    }

    public void setIgnoreReorderedChildren(final boolean ignoreReorderedChildren) {
        this.ignoreReorderedChildren = ignoreReorderedChildren;
    }

    @Override
    public ConfigurationItemCategory getChildCategory(final String childName) {
        // mock the implementation for now ...
        if (getPath().equals("/")) return ConfigurationItemCategory.RUNTIME;
        if (getPath().equals("/path/to/node/runtime-property")) return ConfigurationItemCategory.RUNTIME;
        if (getPath().equals("/path/to/runtime-node")) return ConfigurationItemCategory.RUNTIME;
        if (getPath().startsWith("/path/to/residual-child-nodes-runtime/")) return ConfigurationItemCategory.RUNTIME;

        return ConfigurationItemCategory.CONFIGURATION;
    }

}
