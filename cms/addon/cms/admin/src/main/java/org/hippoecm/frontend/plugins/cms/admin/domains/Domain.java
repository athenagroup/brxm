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
package org.hippoecm.frontend.plugins.cms.admin.domains;

import java.io.Serializable;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.wicket.IClusterable;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.NodeNameCodec;

public class Domain implements Comparable<Domain>, IClusterable {

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private final static String PROP_DESCRIPTION = "hipposys:description";

    private final String path;
    private final String name;
    private String description = "";

    private transient Node node;

    private SortedMap<String, AuthRole> authRoles = new TreeMap<String, AuthRole>();

    public class AuthRole implements Serializable {
        private static final long serialVersionUID = 1L;

        private final String role;
        private SortedSet<String> usernames = new TreeSet<String>();
        private SortedSet<String> groupnames = new TreeSet<String>();

        private transient Node authRoleNode;

        public AuthRole(final Node node) throws RepositoryException {
            this.authRoleNode = node;
            this.role = node.getProperty(HippoNodeType.HIPPO_ROLE).getString();
            if (node.hasProperty(HippoNodeType.HIPPO_USERS)) {
                Value[] vals = node.getProperty(HippoNodeType.HIPPO_USERS).getValues();
                for (Value val : vals) {
                    usernames.add(val.getString());
                }
            }
            if (node.hasProperty(HippoNodeType.HIPPO_GROUPS)) {
                Value[] vals = node.getProperty(HippoNodeType.HIPPO_GROUPS).getValues();
                for (Value val : vals) {
                    groupnames.add(val.getString());
                }
            }
        }

        public String getRole() {
            return role;
        }

        public SortedSet<String> getUsernames() {
            return usernames;
        }

        public SortedSet<String> getGroupnames() {
            return groupnames;
        }

        public void removeGroup(String group) throws RepositoryException {
            groupnames.remove(group);
            authRoleNode.setProperty(HippoNodeType.HIPPO_GROUPS, groupnames.toArray(new String[groupnames.size()]));
            authRoleNode.getSession().save();
        }

        public void addGroup(String group) throws RepositoryException {
            groupnames.add(group);
            authRoleNode.setProperty(HippoNodeType.HIPPO_GROUPS, groupnames.toArray(new String[groupnames.size()]));
            authRoleNode.getSession().save();
        }
    }

    public SortedMap<String, AuthRole> getAuthRoles() {
        return authRoles;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public Domain(final Node node) throws RepositoryException {
        this.path = node.getPath().substring(1);
        this.name = NodeNameCodec.decode(node.getName());
        this.node = node;

        if (node.hasProperty(PROP_DESCRIPTION)) {
            setDescription(node.getProperty(PROP_DESCRIPTION).getString());
        }

        // loop over all authroles
        NodeIterator iter = node.getNodes();
        while (iter.hasNext()) {
            Node child = iter.nextNode();
            if (child != null && child.getPrimaryNodeType().isNodeType(HippoNodeType.NT_AUTHROLE)) {
                AuthRole ar = new AuthRole(child);
                authRoles.put(ar.getRole(), ar);
            }
        }
    }

    //-------------------- persistence helpers ----------//
    public AuthRole createAuthRole(String role) throws RepositoryException {
        Node roleNode = node.addNode(HippoNodeType.NT_AUTHROLE, HippoNodeType.NT_AUTHROLE);
        roleNode.setProperty(HippoNodeType.HIPPO_ROLE, role);
        node.getSession().save();
        AuthRole ar = new AuthRole(roleNode);
        authRoles.put(role, ar);
        return ar;
    }

    public void addGroupToRole(String role, String group) throws RepositoryException {
        if (getAuthRoles().containsKey(role)) {
            getAuthRoles().get(role).addGroup(group);
        } else {
            createAuthRole(role).addGroup(group);
        }
    }

    public void removeGroupFromRole(String role, String group) throws RepositoryException {
        if (getAuthRoles().containsKey(role)) {
            getAuthRoles().get(role).removeGroup(group);
        }
    }

    //--------------------- default object -------------------//
    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || (obj.getClass() != this.getClass())) {
            return false;
        }
        Domain other = (Domain) obj;
        return other.getPath().equals(getPath());
    }

    public int hashCode() {
        return (null == path ? 0 : path.hashCode());
    }
    
    public int compareTo(Domain o) {
        String thisName = getName();
        String otherName = o.getName();
        // 
        int len1 = thisName.length();
        int len2 = otherName.length();
        int n = Math.min(len1, len2);
        char v1[] = thisName.toCharArray();
        char v2[] = otherName.toCharArray();
        int i = 0;
        int j = 0;

        if (i == j) {
            int k = i;
            int lim = n + i;
            while (k < lim) {
            char c1 = v1[k];
            char c2 = v2[k];
            if (c1 != c2) {
                return c1 - c2;
            }
            k++;
            }
        } else {
            while (n-- != 0) {
            char c1 = v1[i++];
            char c2 = v2[j++];
            if (c1 != c2) {
                return c1 - c2;
            }
            }
        }
        return len1 - len2;
    }
}
