/*
 *  Copyright 2008-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.gallery.compare;

import javax.jcr.Property;
import javax.jcr.RepositoryException;

public class MimeTypeComparator extends PropertyComparator {

    public MimeTypeComparator(final String prop) {
        super(prop);
    }

    public MimeTypeComparator(final String prop, final String relPath) {
        super(prop, relPath);
    }

    @Override
    protected int compare(final Property p1, final Property p2) {
        try {
            final String mime1 = p1 == null ? "" : p1.getString();
            final String mime2 = p2 == null ? "" : p2.getString();
            return String.CASE_INSENSITIVE_ORDER.compare(mime1, mime2);
        } catch (final RepositoryException ignored) {
        }

        return 0;
    }
}
