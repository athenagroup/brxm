/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.request;

import java.util.List;
import java.util.Map;

public interface ParameterConfiguration {

    /**
     * Returns the property and if an expression exists it is resolved with the help of the ResolvedSiteMapItem
     */
    String getParameter(String name, ResolvedSiteMapItem hstResolvedSiteMapItem);

    /**
     * @return the ordered list of available parameter names, empty list if there are no parameters
     */
    List<String> getParameterNames();

    /**
     * Returns all resolved parameters into a map
     * @param hstResolvedSiteMapItem
     *
     */
    Map<String, String> getParameters(ResolvedSiteMapItem hstResolvedSiteMapItem);

}
