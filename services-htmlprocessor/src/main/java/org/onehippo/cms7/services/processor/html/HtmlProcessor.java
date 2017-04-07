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
package org.onehippo.cms7.services.processor.html;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import org.onehippo.cms7.services.processor.html.visit.TagVisitor;

public interface HtmlProcessor extends Serializable {

    String read(final String html, final List<TagVisitor> readVisitors) throws IOException;

    String write(final String html, final List<TagVisitor> writeVisitors) throws IOException;

}
