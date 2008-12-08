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
package org.hippoecm.frontend.i18n;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.plugin.IPlugin;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigTraversingPlugin extends AbstractTranslateService implements IPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    final static Logger log = LoggerFactory.getLogger(ConfigTraversingPlugin.class);

    private IPluginConfig translations;

    public ConfigTraversingPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
        this.translations = config.getPluginConfig("hippostd:translations");
    }

    public IModel getModel(Map<String, String> criteria) {
        if (translations != null) {
            IPluginConfig keyConfig = translations.getPluginConfig((String) criteria.get("hippo:key"));
            if (keyConfig != null) {
                Set<IPluginConfig> candidates = keyConfig.getPluginConfigSet();
                Set<ConfigWrapper> list = new HashSet<ConfigWrapper>((int) candidates.size());
                for (IPluginConfig candidate : candidates) {
                    if (candidate.getString("hippo:language", "").equals(criteria.get("hippo:language"))) {
                        list.add(new ConfigWrapper(candidate, criteria));
                    }
                }
                return new TranslationSelectionStrategy<IModel>(criteria.keySet()).select(list).getModel();
            }
        }
        return null;
    }

    @Override
    public void detach() {
        super.detach();
        translations.detach();
    }

}
