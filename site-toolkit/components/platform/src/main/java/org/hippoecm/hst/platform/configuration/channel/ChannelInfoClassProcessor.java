/*
 *  Copyright 2011-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.platform.configuration.channel;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.hippoecm.hst.configuration.channel.ChannelInfo;
import org.hippoecm.hst.configuration.channel.HstPropertyDefinition;
import org.hippoecm.hst.core.parameters.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChannelInfoClassProcessor {

    private static final Logger log = LoggerFactory.getLogger(ChannelInfoClassProcessor.class);

    @SafeVarargs
    public static List<HstPropertyDefinition> getProperties(Class<? extends ChannelInfo> channelInfoClass,
                                                            Class<? extends ChannelInfo>... channelInfoMixins) {
        List<HstPropertyDefinition> properties = new ArrayList<HstPropertyDefinition>();

        if (channelInfoClass != null) {
            addProperties(properties, channelInfoClass);
        }

        if (channelInfoMixins != null) {
            for (Class<? extends ChannelInfo> channelInfoMixin : channelInfoMixins) {
                addProperties(properties, channelInfoMixin);
            }
        }

        return properties;
    }

    private static void addProperties(final List<HstPropertyDefinition> properties, Class<? extends ChannelInfo> channelInfoType) {
        for (Method method : channelInfoType.getMethods()) {
            if (method.isAnnotationPresent(Parameter.class)) {
                // new style annotations
                Parameter propAnnotation = method.getAnnotation(Parameter.class);

                try {
                    HstPropertyDefinition prop = new AnnotationHstPropertyDefinition(propAnnotation, method.getReturnType(), method.getAnnotations());
                    properties.add(prop);
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid parameterized method '{}' : {}", method.getName(), e.toString());
                }
            }
        }
    }
}
