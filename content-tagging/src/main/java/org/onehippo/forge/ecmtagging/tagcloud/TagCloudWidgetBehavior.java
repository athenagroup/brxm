/*
 * Copyright 2010-2019 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.forge.ecmtagging.tagcloud;

import org.hippoecm.frontend.plugins.yui.header.IYuiContext;
import org.hippoecm.frontend.plugins.yui.widget.WidgetBehavior;

public class TagCloudWidgetBehavior extends WidgetBehavior {

    public TagCloudWidgetBehavior() {
        getTemplate().setInstance("YAHOO.hippo.TagCloudWidget");
    }

    @Override
    public void addHeaderContribution(IYuiContext context) {
        context.addModule(TagCloudNamespace.NS, "tagcloud");
        super.addHeaderContribution(context);
    }

}