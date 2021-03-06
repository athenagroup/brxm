/*
 *  Copyright 2009-2019 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.taxonomy.impl;

import static org.onehippo.taxonomy.api.TaxonomyNodeTypes.HIPPOTAXONOMY_CATEGORYINFOS;
import static org.onehippo.taxonomy.api.TaxonomyNodeTypes.NODETYPE_HIPPOTAXONOMY_CATEGORY;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.collections.map.LazyMap;
import org.hippoecm.hst.provider.jcr.JCRValueProviderImpl;
import org.hippoecm.repository.util.NodeIterable;
import org.onehippo.taxonomy.api.Category;
import org.onehippo.taxonomy.api.CategoryInfo;
import org.onehippo.taxonomy.api.Taxonomy;
import org.onehippo.taxonomy.api.TaxonomyException;
import org.onehippo.taxonomy.api.TaxonomyNodeTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CategoryImpl implements Category {

    static Logger log = LoggerFactory.getLogger(CategoryImpl.class);

    private Taxonomy taxonomy;
    private Category parent;
    private List<Category> childCategories = new ArrayList<>();
    private Map<Locale, CategoryInfo> translations = new HashMap<>();
    private String name;
    private String relPath;
    private String key;

    public CategoryImpl(final Node item, final Category parent, final TaxonomyImpl taxonomyImpl) throws RepositoryException, TaxonomyException {

        // Use a ValueProvider, but make sure to clean it up
        final JCRValueProviderImpl jvp = new JCRValueProviderImpl(item);
        this.taxonomy = taxonomyImpl;
        this.parent = parent;
        this.name = jvp.getName();
        final String path = jvp.getPath();
        if (!path.startsWith(taxonomyImpl.getPath() + "/")) {
            throw new TaxonomyException("Path of a category cannot start with other path than root taxonomy");
        }
        this.relPath = path.substring(taxonomyImpl.getPath().length() + 1);

        this.key = jvp.getString(TaxonomyNodeTypes.HIPPOTAXONOMY_KEY);
        jvp.detach();

        if (item.hasNode(HIPPOTAXONOMY_CATEGORYINFOS)) {
            for (Node infoNode : new NodeIterable(item.getNode(HIPPOTAXONOMY_CATEGORYINFOS).getNodes())) {
                CategoryInfo info = new CategoryInfoImpl(infoNode);
                translations.put(info.getLocale(), info);
            }
        }

        // populate children
        for (Node childItem : new NodeIterable(item.getNodes())) {
            if (childItem.isNodeType(NODETYPE_HIPPOTAXONOMY_CATEGORY)) {
                Category taxonomyItem = new CategoryImpl(childItem, this, taxonomyImpl);
                childCategories.add(taxonomyItem);
            } else if (!childItem.isNodeType(HIPPOTAXONOMY_CATEGORYINFOS)) {
                log.warn("Skipping child nodes that are not of type '{}'. Primary node type is '{}'.",
                        NODETYPE_HIPPOTAXONOMY_CATEGORY, childItem.getPrimaryNodeType().getName());
            }
        }

        // if no exception happened, add this item to the descendant list.
        taxonomyImpl.addDescendantItem(this);
    }

    public List<Category> getChildren() {
        return Collections.unmodifiableList(childCategories);
    }

    public String getName() {
        return this.name;
    }

    public String getPath() {
        return this.relPath;
    }

    public String getKey() {
        return this.key;
    }
    
    public Taxonomy getTaxonomy() {
        return this.taxonomy;
    }

    public Category getParent() {
        return this.parent;
    }

    public LinkedList<Category> getAncestors() {
        LinkedList<Category> ancestors = new LinkedList<>();
        Category item = this;
        while (item.getParent() != null) {
            item = item.getParent();
            ancestors.addFirst(item);
        }
        return ancestors;
    }

    @Override
    public CategoryInfo getInfo(final Locale locale) {
        final List<Locale.LanguageRange> documentLocale = Locale.LanguageRange.parse(locale.toLanguageTag());
        final Locale matchingLocale = Locale.lookup(documentLocale, translations.keySet());
        CategoryInfo info = translations.get(matchingLocale);
        if (info == null) {
            return new TransientCategoryInfoImpl(this);
        }
        return info;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<Locale, ? extends CategoryInfo> getInfosByLocale() {
        final Map<Locale, CategoryInfo> map = new HashMap();

        return LazyMap.decorate(map, locale -> getInfo((Locale)locale));
    }
}
