/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository.api;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

public final class WorkflowContext {
    public // FIXME: workaround for current mapping issues
    Session session;
    /**
     * This constructor is not ment for public usage.
     */
    public WorkflowContext(Session session) {
        this.session = session;
    }
    public Document getDocument(String category, String identifier) throws RepositoryException {
        DocumentManager documentManager = ((HippoWorkspace)session.getWorkspace()).getDocumentManager();
        return documentManager.getDocument(category, identifier);
    }
    public String getUsername() {
        return session.getUserID();
    }
}
