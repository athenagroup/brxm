/*
 * Copyright 2014-2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.cms7.essentials.components;

import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.onehippo.cms7.essentials.components.info.EssentialsListComponentInfo;
import org.onehippo.cms7.essentials.components.utils.ComponentsUtils;

import com.google.common.base.Strings;

/**
 * @version "$Id$"
 */
@ParametersInfo(type = EssentialsListComponentInfo.class)
public class EssentialsBlogComponent extends EssentialsListComponent {

    @Override
    public void doBeforeRender(final HstRequest request, final HstResponse response) {
        final EssentialsListComponentInfo paramInfo = getComponentParametersInfo(request);
        final String documentTypes = paramInfo.getDocumentTypes();
        ComponentsUtils.addCurrentDateStrings(request);
        if (Strings.isNullOrEmpty(documentTypes)) {
            setEditMode(request);
            return;
        }
        super.doBeforeRender(request, response);
    }
}