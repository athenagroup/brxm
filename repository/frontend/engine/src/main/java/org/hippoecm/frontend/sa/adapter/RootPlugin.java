/*
 * Copyright 2008 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.sa.adapter;

import org.hippoecm.frontend.LegacyPluginPage;
import org.hippoecm.frontend.model.IPluginModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;

/**
 * @deprecated from the start, needed for handling legacy plugins
 * remove when all legacy plugins have been ported to new services architecture  
 */
@Deprecated
public class RootPlugin extends Plugin {
    private static final long serialVersionUID = 1L;

    private LegacyPluginPage page;

    public RootPlugin(PluginDescriptor pluginDescriptor, IPluginModel model, Plugin parentPlugin) {
        super(pluginDescriptor, model, parentPlugin);

        page = new LegacyPluginPage();
        page.setRootPlugin(this);
    }

    @Override
    public LegacyPluginPage getPluginPage() {
        return page;
    }
}
