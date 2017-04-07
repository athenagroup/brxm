/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.services.processor.html.serialize;

import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.CompactHtmlSerializer;
import org.htmlcleaner.PrettyHtmlSerializer;
import org.htmlcleaner.Serializer;
import org.htmlcleaner.SimpleHtmlSerializer;

public class HtmlSerializerFactory {

    public static Serializer create(final HtmlSerializer serializer) {
        return create(serializer, new CleanerProperties());
    }

    public static Serializer create(final HtmlSerializer serializer, final CleanerProperties properties) {
        switch (serializer) {
            case PRETTY:
                return new PrettyHtmlSerializer(properties) {
                    @Override
                    protected String escapeText(final String content) {
                        return CharacterReferenceNormalizer.normalize(content);
                    }
                };
            case COMPACT:
                return new CompactHtmlSerializer(properties) {
                    @Override
                    protected String escapeText(final String content) {
                        return CharacterReferenceNormalizer.normalize(content);
                    }
                };
            default:
                return new SimpleHtmlSerializer(properties) {
                    @Override
                    protected String escapeText(final String content) {
                        return CharacterReferenceNormalizer.normalize(content);
                    }
                };
        }

    }
}
