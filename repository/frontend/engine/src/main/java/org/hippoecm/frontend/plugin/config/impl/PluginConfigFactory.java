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
package org.hippoecm.frontend.plugin.config.impl;

import java.util.List;

import org.apache.wicket.Application;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.frontend.Main;
import org.hippoecm.frontend.WebApplicationHelper;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfigService;
import org.hippoecm.frontend.session.UserSession;

public class PluginConfigFactory implements IPluginConfigService {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private IPluginConfigService pluginConfigService;

    public PluginConfigFactory(UserSession userSession, IApplicationFactory defaultFactory) {
        IApplicationFactory builtinFactory = new BuiltinApplicationFactory();

        String appName = WebApplicationHelper.getConfigurationParameter((WebApplication) Application.get(),
                Main.PLUGIN_APPLICATION_NAME, null);
        IValueMap credentials = userSession.getCredentials();
        if (UserSession.DEFAULT_CREDENTIALS.equals(credentials)) {
            appName = "login";
        }

        IPluginConfigService baseService;
        try {
            if (appName == null) {
                baseService = defaultFactory.getDefaultApplication();
                if (baseService == null) {
                    baseService = builtinFactory.getDefaultApplication();
                }
            } else {
                baseService = defaultFactory.getApplication(appName);
                if (baseService == null) {
                    baseService = builtinFactory.getDefaultApplication();
                }
            }
        } catch (Exception e) {
            baseService = builtinFactory.getApplication("login");
        }
        pluginConfigService = baseService;
    }

    public IClusterConfig getCluster(String key) {
        return pluginConfigService.getCluster(key);
    }

    public IClusterConfig getDefaultCluster() {
        return pluginConfigService.getDefaultCluster();
    }

    public List<String> listClusters(String folder) {
        return pluginConfigService.listClusters(folder);
    }

    public void detach() {
        pluginConfigService.detach();
    }

}
