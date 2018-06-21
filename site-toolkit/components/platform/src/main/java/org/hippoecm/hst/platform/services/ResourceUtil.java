/*
 * Copyright 2011-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.platform.services;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.hosting.MutableVirtualHost;
import org.hippoecm.hst.configuration.hosting.MutableVirtualHosts;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.core.request.ResolvedVirtualHost;
import org.hippoecm.hst.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility methods for resource classes.
 */
public class ResourceUtil {

    private static final Logger log = LoggerFactory.getLogger(ResourceUtil.class);

    private ResourceUtil() {
        // prevent instantiation
    }

    /**
     *
     * @param virtualHosts the current {@link VirtualHosts} object
     * @param cmsHost the host to match
     * @return the host group name for {@code cmsHost} and {@code null} if the {@code cmsHost} cannot be matched
     */
    public static String getHostGroupNameForCmsHost(final VirtualHosts virtualHosts, final String cmsHost) {

        for (Map.Entry<String, Map<String, MutableVirtualHost>> entry : ((MutableVirtualHosts)virtualHosts).getRootVirtualHostsByGroup().entrySet()) {
            if (entry.getValue().isEmpty()) {
                continue;
            }

            final String hostGroup = entry.getKey();

            // every host within one hostgroup has all the cms locations
            final List<String> cmsLocationsForHostGroup = entry.getValue().values().iterator().next().getCmsLocations();
            for (String cmsLocation : cmsLocationsForHostGroup) {
                try {
                    final URI uri = new URI(cmsLocation);
                    if (StringUtils.substringBefore(cmsHost, ":").equals(uri.getHost())) {
                        return hostGroup;
                    }
                } catch (URISyntaxException e) {
                    log.error("Incorrectly configured cms location '{}' on host group '{}'", cmsLocation, hostGroup);
                    continue;
                }
            }
        }

        return null;
    }




    /**
     * Returns the node with the given UUID using the session of the given request context.
     *
     * @param userSession the session of the current user
     * @param uuidParam a UUID
     *
     * @return the node with the given UUID, or null if no such node could be found.
     */
    public static Node getNode(final Session userSession, final String uuidParam) {
        if (uuidParam == null) {
            log.info("UUID is null, returning null", uuidParam);
            return null;
        }

        final String uuid = PathUtils.normalizePath(uuidParam);

        try {
            UUID.fromString(uuid);
        } catch(IllegalArgumentException e) {
            log.info("Illegal UUID: '{}', returning null", uuidParam);
            return null;
        }

        try {
            return userSession.getNodeByIdentifier(uuid);
        } catch (ItemNotFoundException e) {
            log.warn("Node not found: '{}', returning null", uuid);
        } catch (RepositoryException e) {
            log.warn("Error while fetching node with UUID '" + uuid + "', returning null", e);
        }
        return null;
    }

}
