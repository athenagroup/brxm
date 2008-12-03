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
package org.hippoecm.tools.importer.api;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.repository.api.NodeNameCodec;

public final class PathHelper {

    public static String getParent(String path) {
        if (path.lastIndexOf('/') >= 0) {
            return path.substring(0, path.lastIndexOf('/'));
        } else {
            return null;
        }
    }
    
    public static String encodePath(String path) {
        String[] elements = StringUtils.split(path, '/');
        String[] replacements = new String[elements.length];
        for (int i = 0; i < elements.length; i++) {
            replacements[i] = NodeNameCodec.encode(elements[i]);
        }
        return StringUtils.join(replacements, '/');
    }

}
