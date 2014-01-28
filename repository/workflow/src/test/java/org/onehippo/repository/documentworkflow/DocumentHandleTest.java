/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.repository.documentworkflow;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.RepositoryMap;
import org.hippoecm.repository.reviewedactions.HippoStdPubWfNodeType;
import org.junit.Test;
import org.onehippo.repository.mock.MockNode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DocumentHandleTest {

    protected Node addVariant(Node handle, String state) throws RepositoryException {
        Node variant = handle.addNode(handle.getName(), HippoStdPubWfNodeType.HIPPOSTDPUBWF_DOCUMENT);
        variant.setProperty(HippoStdNodeType.HIPPOSTD_STATE, state);
        return variant;
    }

    protected Node addRequest(Node handle, String type) throws RepositoryException {
        Node variant = handle.addNode(WorkflowRequest.HIPPO_REQUEST, HippoNodeType.NT_REQUEST);
        variant.setProperty(WorkflowRequest.HIPPOSTDPUBWF_TYPE, type);
        return variant;
    }

    @SuppressWarnings("unchecked")
    protected void putWorkflowConfig(RepositoryMap workflowConfig, String key, String value) {
        workflowConfig.put(key, value);
    }

    @Test
    public void initDocumentHandle() throws Exception {

        // create handle with publication request
        MockNode handle = MockNode.root().addMockNode("test", HippoNodeType.NT_HANDLE);
        Node publishRequest = addRequest(handle, WorkflowRequest.PUBLISH);
        DocumentHandle dm = new DocumentHandle(new MockWorkflowContext("testuser"), handle);

        assertEquals("", dm.getStates());
        assertEquals("testuser", dm.getUser());
        assertEquals(publishRequest, dm.getRequest().getNode());


        // add published, unpublished variants & rejected request
        Node publishedVariant = addVariant(handle, HippoStdNodeType.PUBLISHED);
        Node unpublishedVariant = addVariant(handle, HippoStdNodeType.UNPUBLISHED);
        addRequest(handle, WorkflowRequest.REJECTED);
        Node rejectedRequest = addRequest(handle, WorkflowRequest.REJECTED);
        rejectedRequest.setProperty(WorkflowRequest.HIPPOSTDPUBWF_USERNAME, "testuser");
        dm = new DocumentHandle(new MockWorkflowContext("testuser"), handle);

        assertEquals("up", dm.getStates());
        assertNull(dm.getDraft());
        assertEquals(publishedVariant, dm.getPublished().getNode());
        assertEquals(unpublishedVariant, dm.getUnpublished().getNode());
        assertEquals(publishRequest, dm.getRequest().getNode());
        assertEquals(2, dm.getRequests().size());


        // add draft
        Node draftVariant = addVariant(handle, HippoStdNodeType.DRAFT);
        dm = new DocumentHandle(new MockWorkflowContext("testuser"), handle);

        assertEquals("dup", dm.getStates());
        assertNotNull(dm.getDraft());
        assertEquals(draftVariant, dm.getDraft().getNode());
    }

    @Test
    public void testWorkflowSupportedFeatures() throws Exception {
        MockWorkflowContext context = new MockWorkflowContext("testuser");
        MockNode handleNode = MockNode.root().addMockNode("test", HippoNodeType.NT_HANDLE);
        addVariant(handleNode, HippoStdNodeType.DRAFT);
        RepositoryMap workflowConfig = context.getWorkflowConfiguration();

        DocumentHandle dm = new DocumentHandle(context, handleNode);
        assertEquals(DocumentWorkflow.SupportedFeatures.all, dm.getSupportedFeatures());
        assertTrue(dm.getSupportedFeatures().isDocument());
        assertTrue(dm.getSupportedFeatures().isRequest());

        putWorkflowConfig(workflowConfig, "workflow.supportedFeatures", DocumentWorkflow.SupportedFeatures.document.name());
        dm = new DocumentHandle(context, handleNode);
        assertEquals(DocumentWorkflow.SupportedFeatures.document, dm.getSupportedFeatures());
        assertTrue(dm.getSupportedFeatures().isDocument());
        assertFalse(dm.getSupportedFeatures().isRequest());

        putWorkflowConfig(workflowConfig, "workflow.supportedFeatures", "undefined");
        dm = new DocumentHandle(context, handleNode);
        assertEquals(DocumentWorkflow.SupportedFeatures.all, dm.getSupportedFeatures());
    }

    @Test
    public void testPrivileges() throws Exception {
        MockAccessManagedSession session = new MockAccessManagedSession(MockNode.root());
        session.setPermissions("/test/test", "hippo:admin", false);
        MockWorkflowContext context = new MockWorkflowContext("testuser", session);
        Node handleNode = session.getRootNode().addNode("test", HippoNodeType.NT_HANDLE);
        Node draftVariant = addVariant(handleNode, HippoStdNodeType.DRAFT);
        DocumentHandle dm = new DocumentHandle(context, handleNode);

        // testing with only hippo:admin being denied
        assertTrue(dm.isGranted(dm.getDraft(), "foo"));
        assertTrue(dm.isGranted(dm.getDraft(), "foo,bar"));
        assertTrue(dm.isGranted(dm.getDraft(), "bar,foo"));
        assertFalse(dm.isGranted(dm.getDraft(), "hippo:admin"));

        session.setPermissions("/test/test", "hippo:author,hippo:editor", true);
        dm = new DocumentHandle(context, handleNode);

        // testing with only hippo:author,hippo:editor being allowed
        assertFalse(dm.isGranted(dm.getDraft(), "foo"));
        assertFalse(dm.isGranted(dm.getDraft(), "foo,bar"));
        assertFalse(dm.isGranted(dm.getDraft(), "bar,foo"));
        assertFalse(dm.isGranted(dm.getDraft(), "hippo:author,foo"));
        assertFalse(dm.isGranted(dm.getDraft(), "hippo:author,hippo:editor,foo"));
        assertTrue(dm.isGranted(dm.getDraft(), "hippo:author"));
        assertTrue(dm.isGranted(dm.getDraft(), "hippo:editor"));
        assertTrue(dm.isGranted(dm.getDraft(), "hippo:author,hippo:editor"));
        assertTrue(dm.isGranted(dm.getDraft(), "hippo:editor,hippo:author"));
        assertFalse(dm.isGranted(dm.getDraft(), "hippo:admin"));
        assertFalse(dm.isGranted(dm.getDraft(), "hippo:admin,foo"));
        assertFalse(dm.isGranted(dm.getDraft(), "hippo:admin,hippo:author"));
        assertFalse(dm.isGranted(dm.getDraft(), "hippo:admin,hippo:author,hippo:editor"));
        assertFalse(dm.isGranted(dm.getDraft(), "hippo:author,hippo:editor,hippo:admin"));
    }
}
