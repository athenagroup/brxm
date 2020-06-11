/*
 *  Copyright 2018-2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.platform.model;

import java.util.List;

import javax.servlet.ServletContext;

import org.hippoecm.hst.core.container.ComponentManager;

public interface HstModelRegistry {

    HstModel registerHstModel(ServletContext servletContext, ComponentManager websiteComponentManager,
                              boolean loadHstConfigNodes) throws ModelRegistrationException;

    void unregisterHstModel(String contextPath) throws ModelRegistrationException;

    void unregisterHstModel(ServletContext servletContext) throws ModelRegistrationException;

    HstModel getHstModel(String contextPath);

    HstModel getHstModel(ClassLoader classLoader);

    List<HstModel> getHstModels();
}