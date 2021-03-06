/*
 *  Copyright 2018-2019 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms.channelmanager.content.picker;

import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeContext;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class NodePicker {

    static final String[] BOOLEAN_PROPERTIES = {
            "language.context.aware"
    };

    private static final String[] STRING_PROPERTIES = {
            "base.path",
    };

    private NodePicker() {
    }

    public static ObjectNode build(final FieldTypeContext fieldContext) {
        return Picker.configure(fieldContext)
                .booleans(BOOLEAN_PROPERTIES)
                .strings(STRING_PROPERTIES)
                .build();
    }
}
