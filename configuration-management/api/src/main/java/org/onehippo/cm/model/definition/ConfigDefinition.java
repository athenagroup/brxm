/*
 *  Copyright 2016-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.model.definition;

import org.onehippo.cm.model.source.ConfigSource;
import org.onehippo.cm.model.source.Source;
import org.onehippo.cm.model.source.SourceType;

/**
 * Represents the definition of a JCR node tree in a {@link Source} of type {@link SourceType#CONFIG}, including metadata.
 * {@link #getType()} always returns {@link DefinitionType#CONFIG}.
 */
public interface ConfigDefinition<S extends ConfigSource> extends TreeDefinition<S> {
}
