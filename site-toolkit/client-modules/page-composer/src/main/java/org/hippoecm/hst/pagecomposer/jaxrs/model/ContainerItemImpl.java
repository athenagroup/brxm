/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.pagecomposer.jaxrs.model;

import javax.jcr.Node;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;

public class ContainerItemImpl implements ContainerItem {

    private final Node containerItem;
    private final HstComponentConfiguration componentDefinition;
    private final long timeStamp;


    public ContainerItemImpl(final Node containerItem,
                             final HstComponentConfiguration componentDefinition, final long timeStamp) {
        this.containerItem = containerItem;
        this.componentDefinition = componentDefinition;
        this.timeStamp = timeStamp;
    }

    @Override
    public Node getContainerItem() {
        return containerItem;
    }

    @Override
    public HstComponentConfiguration getComponentDefinition() {
        return componentDefinition;
    }

    @Override
    public long getTimeStamp() {
        return timeStamp;
    }

}
