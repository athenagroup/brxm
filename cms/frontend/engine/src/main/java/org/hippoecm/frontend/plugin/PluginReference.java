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
package org.hippoecm.frontend.plugin;

import org.apache.wicket.model.LoadableDetachableModel;
import org.hippoecm.frontend.Home;

public class PluginReference extends LoadableDetachableModel {
    private static final long serialVersionUID = 1L;

    private Home page;
    private String pluginPath;

    public PluginReference(Plugin plugin) {
        super(plugin);

        page = plugin.getPluginPage();
        pluginPath = plugin.getPluginPath();
    }

    public Plugin getPlugin() {
        return (Plugin) getObject();
    }

    @Override
    public Object load() {
        Plugin root = page.getRootPlugin();
        return root.getChildPlugin(pluginPath);
    }
}
