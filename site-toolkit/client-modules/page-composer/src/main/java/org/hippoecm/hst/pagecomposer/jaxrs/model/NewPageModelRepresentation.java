/*
 *  Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagecomposer.jaxrs.model;

import java.util.ArrayList;
import java.util.List;

public class NewPageModelRepresentation {

    private List<PrototypeRepresentation> prototypes = new ArrayList<>();
    private List<SiteMapPageRepresentation> locations = new ArrayList<>();


    public NewPageModelRepresentation() {}

    public List<PrototypeRepresentation> getPrototypes() {
        return prototypes;
    }

    public void setPrototypes(final List<PrototypeRepresentation> prototypes) {
        this.prototypes = prototypes;
    }

    public List<SiteMapPageRepresentation> getLocations() {
        return locations;
    }

    public void setLocations(final List<SiteMapPageRepresentation> locations) {
        this.locations = locations;
    }
}
