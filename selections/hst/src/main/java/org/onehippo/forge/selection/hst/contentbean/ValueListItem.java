/*
 * Copyright 2010-2019 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.forge.selection.hst.contentbean;

import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.standard.HippoItem;
import org.onehippo.cms7.essentials.dashboard.annotations.HippoEssentialsGenerated;

/**
 * [selection:listitem] 
 *  - selection:key (string) 
 *  - selection:label (string) 
 */
@Node(jcrType = "selection:listitem")
@HippoEssentialsGenerated(allowModifications = false)
public class ValueListItem extends HippoItem  {

    public String getKey() {
        return getProperty("selection:key");
    }

    public String getLabel() {
        return getProperty("selection:label");
    }
}