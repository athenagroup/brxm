/*
 *  Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.migration;

import java.util.ArrayList;
import java.util.List;

public class EsvProperty {

    private String name;
    private int type;
    private Boolean multiple;
    private EsvMerge merge;
    private String mergeLocation;
    private List<EsvValue> values = new ArrayList<>();

    public EsvProperty(final String name, final int type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public int getType() {
        return type;
    }

    public Boolean getMultiple() {
        return multiple;
    }

    public Boolean isMultiple() {
        return multiple != null && multiple;
    }

    public void setMultiple(final Boolean multiple) {
        this.multiple = multiple;
    }

    public EsvMerge getMerge() {
        return merge;
    }

    public void setMerge(final EsvMerge merge) {
        this.merge = merge;
    }

    public String getMergeLocation() {
        return mergeLocation;
    }

    public void setMergeLocation(final String mergeLocation) {
        this.mergeLocation = mergeLocation;
    }

    public List<EsvValue> getValues() {
        return values;
    }
}
