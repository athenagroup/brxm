/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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
 *
 */

package org.hippoecm.hst.pagecomposer.jaxrs.services;

import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.channel.Channel;
import org.hippoecm.hst.configuration.channel.ChannelException;
import org.hippoecm.hst.configuration.channel.ChannelInfo;
import org.hippoecm.hst.configuration.channel.ChannelInfoClassProcessor;
import org.hippoecm.hst.configuration.channel.ChannelPropertyMapper;
import org.hippoecm.hst.configuration.hosting.VirtualHost;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.rest.beans.ChannelInfoClassInfo;
import org.hippoecm.hst.rest.beans.InformationObjectsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChannelServiceImpl implements ChannelService {
    private static final Logger log = LoggerFactory.getLogger(ChannelServiceImpl.class);

    @Override
    public ChannelInfoClassInfo getChannelInfo(final String channelId) throws ChannelException {
        try {
            Class<? extends ChannelInfo> channelInfoClass = getVirtualHosts().getChannelInfoClass(getCurrentVirtualHost().getHostGroupName(), channelId);
            ChannelInfoClassInfo channelInfoClassInfo = null;
            if (channelInfoClass != null) {
                channelInfoClassInfo = InformationObjectsBuilder.buildChannelInfoClassInfo(channelInfoClass);
            }
            return channelInfoClassInfo;
        } catch (ChannelException e) {
            if (log.isDebugEnabled()) {
                log.info("Failed to retrieve channel info class for channel with id '{}'", channelId, e);
            } else {
                log.info("Failed to retrieve channel info class for channel with id '{}'", channelId, e.toString());
            }
            throw e;
        }
    }

    @Override
    public Channel getChannel(final String channelId) {
        final VirtualHost virtualHost = getCurrentVirtualHost();
        return getVirtualHosts().getChannelById(virtualHost.getHostGroupName(), channelId);
    }

    @Override
    public void saveChannelProperties(Session session, final String channelId, Map<String, Object> properties) throws RepositoryException, IllegalStateException {
        final Channel channel = getChannel(channelId);
        final String channelPath = channel.getChannelPath();
        if (StringUtils.isEmpty(channelPath)) {
            throw new IllegalStateException("Path for the channel '" + channel.getId() + "' not found");
        }

        final Node channelNode = session.getNode(channelPath);
        final Node channelPropsNode = getOrAddChannelPropsNode(channelNode);

        final String channelInfoClassName = channel.getChannelInfoClassName();
        try {
            Class<? extends ChannelInfo> channelInfoClass = (Class<? extends ChannelInfo>) ChannelPropertyMapper.class.getClassLoader().loadClass(channelInfoClassName);
            ChannelPropertyMapper.saveProperties(channelPropsNode, ChannelInfoClassProcessor.getProperties(channelInfoClass), properties);
        } catch (ClassNotFoundException e) {
            log.warn("Could not find channel info class " + channelInfoClassName, e);
            throw new IllegalStateException("Could not find channel info class ", e);
        }
    }

    private Node getOrAddChannelPropsNode(final Node channelNode) throws RepositoryException {
        if (!channelNode.hasNode(HstNodeTypes.NODENAME_HST_CHANNELINFO)) {
            return channelNode.addNode(HstNodeTypes.NODENAME_HST_CHANNELINFO, HstNodeTypes.NODETYPE_HST_CHANNELINFO);
        } else {
            return channelNode.getNode(HstNodeTypes.NODENAME_HST_CHANNELINFO);
        }
    }

    private VirtualHost getCurrentVirtualHost() {
        return RequestContextProvider.get().getResolvedMount().getMount().getVirtualHost();
    }

    private VirtualHosts getVirtualHosts() {
        return RequestContextProvider.get().getVirtualHost().getVirtualHosts();
    }
}
