/*
 *  Copyright 2009-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.taxonomy.plugin.api;

import java.util.List;
import java.util.Locale;

import org.onehippo.taxonomy.api.Taxonomy;
import org.onehippo.taxonomy.api.TaxonomyException;

public interface EditableTaxonomy extends Taxonomy {

    /**
     * @deprecated use {@link #addCategory(String, String, Locale)} instead
     */
    @Deprecated
    EditableCategory addCategory(String key, String name, String locale) throws TaxonomyException;

    EditableCategory addCategory(String key, String name, Locale locale) throws TaxonomyException;

    List<? extends EditableCategory> getCategories();

    List<? extends EditableCategory> getDescendants();

    EditableCategory getCategory(String relPath);

    EditableCategory getCategoryByKey(String key);

}
