/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.dashboard.utils;

import org.hippoecm.repository.api.HippoNodeType;
import org.junit.Test;
import org.onehippo.cms7.essentials.BaseRepositoryTest;

import static org.junit.Assert.assertTrue;

/**
 * @version "$Id$"
 */
public class CndUtilsTest extends BaseRepositoryTest {

    public static final String TEST_URI = "http://www.test.com";
    public static final String TEST_PREFIX = "test";

    @Test
    public void testRegisterNamespaceUri() throws Exception {

        session.getRootNode().addNode(HippoNodeType.NAMESPACES_PATH);
        session.save();
        CndUtils.registerNamespace(getContext(), TEST_PREFIX, TEST_URI);
        assertTrue("CndUtils.registerNamespaceUri", true);
        CndUtils.createHippoNamespace(getContext(), TEST_PREFIX);
        assertTrue("CndUtils.createHippoNamespace", true);
        boolean exists = CndUtils.existsNamespaceUri(getContext(), TEST_URI);
        assertTrue(exists);
        CndUtils.registerDocumentType(getContext(), TEST_PREFIX, "myname", false, false, GalleryUtils.HIPPOGALLERY_IMAGE_SET, GalleryUtils.HIPPOGALLERY_RELAXED);
        assertTrue("CndUtils.registerDocumentType", true);
        // test un-register type
        boolean removed = CndUtils.unRegisterDocumentType(getContext(),TEST_PREFIX, "myname");
        assertTrue("Expected type to be removed", removed);


    }



}
