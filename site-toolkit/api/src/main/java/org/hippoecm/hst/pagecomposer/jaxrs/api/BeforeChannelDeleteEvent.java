/*
 *  Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagecomposer.jaxrs.api;

import java.util.List;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.onehippo.cms7.services.hst.Channel;

/**
 * Channel event occuring before a channel is deleted.
 */
public interface BeforeChannelDeleteEvent extends BaseChannelEvent {

    /**
     * Return the channel where this event occurs on.
     * @return the channel where this event occurs on
     */
    public Channel getChannel();

    /**
     * @return the {@link HstRequestContext} that is used that triggers this this channel event
     */
    public HstRequestContext getRequestContext();

    /**
     * Return all mounts binding to the deleting channel.
     */
    public List<Mount> getMounts();

}
