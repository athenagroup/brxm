/*
 * Copyright 2014-2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.hst.demo.events;

import net.sf.json.JSONObject;

import org.onehippo.cms7.event.HippoEvent;
import org.onehippo.cms7.services.eventbus.HippoEventListenerRegistry;
import org.onehippo.cms7.services.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChannelManagerEventListener {

    private static Logger log = LoggerFactory.getLogger(ChannelManagerEventListener.class);

    public void register() {
        HippoEventListenerRegistry.get().register(this);
    }

    public void unregister() {
        HippoEventListenerRegistry.get().unregister(this);
    }

    @Subscribe
    @SuppressWarnings("unused")
    public void onEvent(final HippoEvent event) {
        if ("channel-manager".equals(event.category())) {
            if (log.isInfoEnabled()) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.putAll(event.getValues());
                log.info("channel-manager event handled: {}", jsonObject);
            }
        }
    }
}
