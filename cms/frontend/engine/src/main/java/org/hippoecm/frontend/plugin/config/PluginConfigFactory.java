/*
 * Copyright 2007 Hippo
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
package org.hippoecm.frontend.plugin.config;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.Application;
import org.apache.wicket.Session;
import org.hippoecm.frontend.Main;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNodeType;

public class PluginConfigFactory {

    private PluginConfig pluginConfig;

    public PluginConfigFactory() {
        try {
            UserSession session = (UserSession) Session.get();
            Node rootNode = session.getJcrSession().getRootNode();
            String path = HippoNodeType.CONFIGURATION_PATH + "/" + HippoNodeType.FRONTEND_PATH;
            Node configNode = rootNode.getNode(path);

            Main main = (Main) Application.get();
            String application = main.getHippoApplication();
            boolean hipposAvailable = configNode.getNodes().hasNext();
            boolean builtIn = application.contains("(builtin)");

            if (!builtIn && hipposAvailable) {
                pluginConfig = new PluginRepositoryConfig(path + "/" + application);
            } else {
                pluginConfig = new PluginJavaConfig();
            }
        } catch (RepositoryException e) {
            pluginConfig = new PluginJavaConfig();
        }

    }

    public PluginConfig getPluginConfig() {
        return pluginConfig;
    }

}
