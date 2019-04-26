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
package org.hippoecm.hst;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hippoecm.hst.content.beans.*;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.manager.VersionedObjectConverterProxy;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.junit.BeforeClass;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.contenttype.ContentTypeService;
import org.onehippo.cms7.services.contenttype.ContentTypes;
import org.onehippo.repository.testutils.RepositoryTestCase;

public abstract class AbstractBeanTestCase extends RepositoryTestCase {

    @BeforeClass
    public static void setUpClass() throws Exception {
        //Enable legacy project structure mode (without extensions)
        System.setProperty("use.hcm.sites", "false");
        RepositoryTestCase.setUpClass();
    }


    protected ObjectConverter getObjectConverter() {
        return getObjectConverter((List<Class<? extends HippoBean>>) getAnnotatedClasses());
    }
 
    protected ObjectConverter getObjectConverter(List<Class<? extends HippoBean>> annotatedClasses) {
        return new VersionedObjectConverterProxy(annotatedClasses, annotatedClasses, new ContentTypesProvider() {
            @Override
            public ContentTypes getContentTypes() {
                try {
                    final ContentTypes contentTypes = HippoServiceRegistry.getService(ContentTypeService.class).getContentTypes();
                    return contentTypes;
                } catch (Exception e) {
                    throw new IllegalStateException("ContentTypeService is unavailable!");
                }
            }
        }, true);
    }

    protected Collection<Class<? extends HippoBean>> getAnnotatedClasses() {
        List<Class<? extends HippoBean>> annotatedClasses = new ArrayList<Class<? extends HippoBean>>();
        annotatedClasses.add(PersistableTextPage.class);
        annotatedClasses.add(NewsPage.class);
        annotatedClasses.add(BasePage.class);
        annotatedClasses.add(PersistableTextPageCopy.class);
        annotatedClasses.add(ContentDocument.class);
        annotatedClasses.add(BaseDocument.class);
        annotatedClasses.add(LinkDepthTestDocument.class);
        annotatedClasses.add(DeepLink.class);
        annotatedClasses.add(DeeperLink.class);
        annotatedClasses.add(DeepestLink.class);
        annotatedClasses.add(TooDeepLink.class);
        return annotatedClasses;
    }
    
}
