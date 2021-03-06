/*
 *  Copyright 2008-2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.security;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.jackrabbit.api.security.user.UserManager;
import org.hippoecm.repository.security.group.DummyGroupManager;
import org.hippoecm.repository.security.group.GroupManager;
import org.hippoecm.repository.security.user.DummyUserManager;
import org.hippoecm.repository.security.user.HippoUserManager;

public abstract class AbstractSecurityProvider implements SecurityProvider {

    protected UserManager userManager = new DummyUserManager();
    protected GroupManager groupManager = new DummyGroupManager();

    public void remove() {
    }

    public UserManager getUserManager() {
        return userManager;
    }

    public GroupManager getGroupManager() {
        return groupManager;
    }

    public UserManager getUserManager(Session session) throws RepositoryException {
        return userManager;
    }

    public GroupManager getGroupManager(Session session) throws RepositoryException {
        return groupManager;
    }

    public void synchronizeOnLogin(SimpleCredentials creds) throws RepositoryException {
        // The sync blocks are synchronized because the underlying
        // methods can share the same jcr session and the jcr session is
        // not thread safe. This is a "best effort" solution as the usrMgr
        // and the groupMgr could also share the same session but generally
        // do not operate on the same nodes.

        final HippoUserManager userMgr = (HippoUserManager) getUserManager();
        syncUser(creds, userMgr);

        final GroupManager groupMgr = getGroupManager();
        syncGroup(creds, userMgr, groupMgr);
    }

    protected void syncUser(final SimpleCredentials creds, final HippoUserManager userMgr) throws RepositoryException {
        final String userId = creds.getUserID();
        synchronized(userMgr) {
            userMgr.syncUserInfo(userId);
            userMgr.updateLastLogin(userId);
            userMgr.saveUsers();
        }
    }

    protected void syncGroup(final SimpleCredentials creds, final HippoUserManager userMgr, final GroupManager groupMgr) throws RepositoryException {
        synchronized(groupMgr) {
            groupMgr.syncMemberships(userMgr.getUser(creds.getUserID()));
            groupMgr.saveGroups();
        }
    }

}
