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
package org.onehippo.cm.impl.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.jackrabbit.util.Text;
import org.onehippo.cm.api.MergedModel;
import org.onehippo.cm.api.model.Definition;
import org.onehippo.cm.api.model.Module;
import org.onehippo.cm.api.model.Project;
import org.onehippo.cm.api.model.Source;

public class ModuleImpl implements Module, Comparable<Module> {

    private final String name;
    private final Project project;

    /**
     * Warning! not preserved in final MergedModel!
     */
    private final Set<String> modifiableAfter = new LinkedHashSet<>();
    private final Set<String> after = Collections.unmodifiableSet(modifiableAfter);

    private final Set<SourceImpl> sortedSources = new TreeSet<>(Comparator.comparing(Source::getPath));

    private final Set<Source> sources = Collections.unmodifiableSet(sortedSources);

    private final List<NamespaceDefinitionImpl> namespaceDefinitions = new ArrayList<>();
    private final List<NodeTypeDefinitionImpl> nodeTypeDefinitions = new ArrayList<>();
    private final List<ContentDefinitionImpl> contentDefinitions = new ArrayList<>();
    private final List<WebFileBundleDefinitionImpl> webFileBundleDefinitions = new ArrayList<>();

    public ModuleImpl(final String name, final ProjectImpl project) {
        if (name == null) {
            throw new IllegalArgumentException("Parameter 'name' cannot be null");
        }
        this.name = name;

        if (project == null) {
            throw new IllegalArgumentException("Parameter 'project' cannot be null");
        }
        this.project = project;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Project getProject() {
        return project;
    }

    @Override
    public Set<String> getAfter() {
        return after;
    }

    public ModuleImpl addAfter(final Set<String> after) {
        modifiableAfter.addAll(after);
        return this;
    }

    @Override
    public Set<Source> getSources() {
        return sources;
    }

    @Override
    public Set<Source> getContentSources() {
        return sources.stream().filter(x -> x instanceof ContentSourceImpl).collect(Collectors.toSet());
    }

    @Override
    public Set<Source> getConfigSources() {
        return sources.stream().filter(x -> x instanceof ConfigSourceImpl).collect(Collectors.toSet());
    }

    @Deprecated
    private SourceImpl addSource(final String path) {
        final SourceImpl source = new ConfigSourceImpl(path, this);
        return addSource(source);
    }

    public SourceImpl addContentSource(final String path) {
        final SourceImpl source = new ContentSourceImpl(path, this);
        return addSource(source);
    }

    public SourceImpl addConfigSource(final String path) {
        final SourceImpl source = new ConfigSourceImpl(path, this);
        return addSource(source);
    }

    /**
     * Returns existing or adds new source to the source list
     * @param source
     * @return
     */
    private SourceImpl addSource(SourceImpl source) {
        return sortedSources.add(source) ? source : sortedSources
                .stream()
                .filter(s -> s.getPath().equals(source.getPath()) && s.getClass().equals(source.getClass()))
                .findFirst().get();
    }

    public Set<SourceImpl> getModifiableSources() {
        return sortedSources;
    }

    /**
     * @return a sorted list of namespace definitions in insertion order.
     * Note that these definitions are only populated for Modules that are part of the {@link MergedModel}.
     */
    public List<NamespaceDefinitionImpl> getNamespaceDefinitions() {
        return namespaceDefinitions;
    }

    /**
     * @return a sorted list of node type definitions in insertion order.
     * Note that these definitions are only populated for Modules that are part of the {@link MergedModel}.
     */
    public List<NodeTypeDefinitionImpl> getNodeTypeDefinitions() {
        return nodeTypeDefinitions;
    }

    /**
     * @return a sorted list of content (or config) definitions.
     * Note that these definitions are only populated for Modules that are part of the {@link MergedModel}.
     */
    public List<ContentDefinitionImpl> getContentDefinitions() {
        return contentDefinitions;
    }

    /**
     * @return a sorted list of web file bundle definitions in insertion order.
     * Note that these definitions are only populated for Modules that are part of the {@link MergedModel}.
     */
    public List<WebFileBundleDefinitionImpl> getWebFileBundleDefinitions() {
        return webFileBundleDefinitions;
    }

    void pushDefinitions(final ModuleImpl module) {
        this.sortedSources.addAll(module.sortedSources);

        // sort definitions into the different types
        module.getSources().forEach(source ->
                source.getDefinitions().forEach(definition -> {
                    if (definition instanceof NamespaceDefinitionImpl) {
                        namespaceDefinitions.add((NamespaceDefinitionImpl) definition);
                    } else if (definition instanceof NodeTypeDefinitionImpl) {
                        ensureSingleSourceForNodeTypes(definition);
                        nodeTypeDefinitions.add((NodeTypeDefinitionImpl) definition);
                    } else if (definition instanceof ContentDefinitionImpl) {
                        contentDefinitions.add((ContentDefinitionImpl) definition);
                    } else if (definition instanceof WebFileBundleDefinitionImpl) {
                        webFileBundleDefinitions.add((WebFileBundleDefinitionImpl) definition);
                    } else {
                        throw new IllegalStateException("Failed to sort unsupported definition class '"
                                + definition.getClass().getName() + "'.");
                    }
                })
        );

        // sort the content/config definitions, all other remain in insertion order
        contentDefinitions.sort(new ContentDefinitionComparator());
    }

    private void ensureSingleSourceForNodeTypes(final Definition nodeTypeDefinition) {
        if (!nodeTypeDefinitions.isEmpty()
                && !nodeTypeDefinition.getSource().getPath().equals(nodeTypeDefinitions.get(0).getSource().getPath())) {
            final String msg = String.format("CNDs are specified in multiple sources of a module: '%s' and '%s'. "
                    + "For proper ordering, they must be specified in a single source.",
                    ModelUtils.formatDefinition(nodeTypeDefinition),
                    ModelUtils.formatDefinition(nodeTypeDefinitions.get(0)));
            throw new IllegalStateException(msg);
        }
    }

    private class ContentDefinitionComparator implements Comparator<ContentDefinitionImpl> {
        public int compare(final ContentDefinitionImpl def1, final ContentDefinitionImpl def2) {
            final String rootPath1 = def1.getNode().getPath();
            final String rootPath2 = def2.getNode().getPath();

            if (def1 != def2 && rootPath1.equals(rootPath2)) {
                final String msg = String.format(
                        "Duplicate content root paths '%s' in module '%s' in source files '%s' and '%s'.",
                        rootPath1,
                        getName(),
                        ModelUtils.formatDefinition(def1),
                        ModelUtils.formatDefinition(def2));
                throw new IllegalStateException(msg);
            }
            return rootPath1.compareTo(rootPath2);
        }
    }

    @Override
    public String toString() {
        return "ModuleImpl{" +
                "name='" + name + '\'' +
                ", project=" + project +
                '}';
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof Module) {
            Module otherModule = (Module)other;
            return this.getName().equals(otherModule.getName()) &&
                    this.getProject().equals(otherModule.getProject());
        }
        return false;
    }

    // hashCode() and equals() should be consistent!
    @Override
    public int hashCode() {
        return Objects.hash(name, project);
    }

    /**
     * @return the full group/project/module name for this module
     */
    public String getFullName() {
        return buildFullName(this);
    }

    /**
     * @return the full group/project/module name for the given module
     */
    public static String buildFullName(Module m) {
        return String.join("/",
                m.getProject().getConfiguration().getName(),
                m.getProject().getName(),
                m.getName());
    }

    /**
     * Compare on the basis of lexical order of the full group/project/module names.
     * @param o another Module
     */
    @Override
    public int compareTo(final Module o) {
        return getFullName().compareTo(buildFullName(o));
    }
}
