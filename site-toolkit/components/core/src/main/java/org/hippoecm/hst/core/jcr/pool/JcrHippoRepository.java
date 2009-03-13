/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.hst.core.jcr.pool;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.hippoecm.repository.HippoRepository;

public class JcrHippoRepository implements Repository {
    
    protected HippoRepository hippoRepository;

    public JcrHippoRepository(HippoRepository hippoRepository) {
        this.hippoRepository = hippoRepository;
    }
    
    public String getDescriptor(String key) {
        return this.hippoRepository.getRepository().getDescriptor(key);
    }

    public String[] getDescriptorKeys() {
        return this.hippoRepository.getRepository().getDescriptorKeys();
    }

    public Session login() throws LoginException, RepositoryException {
        return this.hippoRepository.login();
    }

    public Session login(Credentials credentials) throws LoginException, RepositoryException {
        return this.hippoRepository.login((SimpleCredentials) credentials);
    }

    public Session login(String workspaceName) throws LoginException, NoSuchWorkspaceException, RepositoryException {
        return login();
    }

    public Session login(Credentials credentials, String workspaceName) throws LoginException, NoSuchWorkspaceException,
            RepositoryException {
        return login(credentials);
    }
    
}
