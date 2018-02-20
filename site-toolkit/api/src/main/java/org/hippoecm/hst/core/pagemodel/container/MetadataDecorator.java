/*
 *  Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.pagemodel.container;

import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.pagemodel.model.MetadataContributable;

/**
 * Decorator interface to allow custom decorators to add more metadata.
 */
public interface MetadataDecorator {

    /**
     * Decorate the given component window's {@code metadataModel}, either component window or container window.
     * @param request HstRequest instance
     * @param response HstResponse instance
     * @param metadataModel metadata model object to decorate
     */
    public void decorateComponentWindowMetadata(HstRequest request, HstResponse response,
            MetadataContributable metadataModel);

    /**
     * Decorate the given content bean's {@code metadataModel}.
     * @param request HstRequest instance
     * @param response HstResponse instance
     * @param contentBean content bean object
     * @param metadataModel metadata model object to decorate
     */
    public void decorateContentMetadata(HstRequest request, HstResponse response, HippoBean contentBean,
            MetadataContributable metadataModel);

}
