/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cm;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.onehippo.cm.model.ConfigurationModel;
import org.onehippo.cms7.services.SingletonService;

/**
 * Service providing access to the current runtime ConfigurationModel
 */
@SingletonService
public interface ConfigurationService {

    /**
     * Retrieve the current (partial) runtime ConfigurationModel This model will not contain
     * content definitions, which are not stored/retained in the runtime ConfigurationModel.
     */
    ConfigurationModel getRuntimeConfigurationModel();

    /**
     * @return Returns true if AutoExport is allowed (might be disable though)
     */
    boolean isAutoExportAvailable();

    /**
     * Export node and it's binaries to zip file.
     * @param nodeToExport {@link Node}
     * @return Zip {@link File} containing content wit binaries. File must be deleted by caller
     */
    File exportZippedContent(Node nodeToExport) throws RepositoryException, IOException;

    /**
     * Export node text only
     * @param nodeToExport {@link Node}
     * @return Textual representation of the node
     */
    String exportContent(Node nodeToExport) throws RepositoryException, IOException;

    /**
     * Import node and it's binaries into parent node
     * @param zipFile zip {@link File}
     * @param parentNode parent {@link Node}
     */
    void importZippedContent(File zipFile, Node parentNode) throws RepositoryException, IOException;

    /**
     * Import single content definition
     * @param inputStream {@link InputStream} representation of yaml
     * @param parentNode parent {@link Node}
     */
    void importPlainYaml(final InputStream inputStream, final Node parentNode) throws RepositoryException;
}
