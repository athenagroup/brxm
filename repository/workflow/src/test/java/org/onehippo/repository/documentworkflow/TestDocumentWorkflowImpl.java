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

import java.io.Serializable;
import java.util.Calendar;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.scxml2.model.TransitionTarget;
import org.apache.jackrabbit.util.ISO8601;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.RepositoryMap;
import org.hippoecm.repository.reviewedactions.HippoStdPubWfNodeType;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.repository.documentworkflow.action.ArchiveAction;
import org.onehippo.repository.documentworkflow.action.CopyDocumentAction;
import org.onehippo.repository.documentworkflow.action.CopyVariantAction;
import org.onehippo.repository.scxml.ActionAction;
import org.onehippo.repository.scxml.ResultAction;
import org.onehippo.repository.documentworkflow.action.InfoAction;
import org.onehippo.repository.documentworkflow.action.IsModifiedAction;
import org.onehippo.repository.documentworkflow.action.MoveDocumentAction;
import org.onehippo.repository.documentworkflow.action.RenameDocumentAction;
import org.onehippo.repository.documentworkflow.action.RequestAction;
import org.onehippo.repository.documentworkflow.action.ScheduleRequestAction;
import org.onehippo.repository.mock.MockNode;
import org.onehippo.repository.mock.MockValue;
import org.onehippo.repository.mock.MockVersion;
import org.onehippo.repository.scxml.MockRepositorySCXMLRegistry;
import org.onehippo.repository.scxml.RepositorySCXMLExecutorFactory;
import org.onehippo.repository.scxml.SCXMLExecutorFactory;
import org.onehippo.repository.scxml.SCXMLRegistry;
import org.onehippo.repository.scxml.SCXMLWorkflowExecutor;
import org.onehippo.repository.util.JcrConstants;

import static org.junit.Assert.assertEquals;

public class TestDocumentWorkflowImpl {

    @BeforeClass
    public static void beforeClass() throws Exception {
        MockRepositorySCXMLRegistry registry = new MockRepositorySCXMLRegistry();
        MockNode scxmlConfigNode = registry.createConfigNode();
        MockNode scxmlNode = registry.addScxmlNode(scxmlConfigNode, "document-workflow", loadTestSCXML());
        registry.addCustomAction(scxmlNode, "http://www.onehippo.org/cms7/repository/scxml", "action", ActionAction.class.getName());
        registry.addCustomAction(scxmlNode, "http://www.onehippo.org/cms7/repository/scxml", "result", ResultAction.class.getName());
        registry.addCustomAction(scxmlNode, "http://www.onehippo.org/cms7/repository/scxml", "info", InfoAction.class.getName());
        registry.addCustomAction(scxmlNode, "http://www.onehippo.org/cms7/repository/scxml", "copyvariant", CopyVariantAction.class.getName());
        registry.addCustomAction(scxmlNode, "http://www.onehippo.org/cms7/repository/scxml", "request", RequestAction.class.getName());
        registry.addCustomAction(scxmlNode, "http://www.onehippo.org/cms7/repository/scxml", "archive", ArchiveAction.class.getName());
        registry.addCustomAction(scxmlNode, "http://www.onehippo.org/cms7/repository/scxml", "ismodified", IsModifiedAction.class.getName());
        registry.addCustomAction(scxmlNode, "http://www.onehippo.org/cms7/repository/scxml", "schedulerequest", ScheduleRequestAction.class.getName());
        registry.addCustomAction(scxmlNode, "http://www.onehippo.org/cms7/repository/scxml", "copydocument", CopyDocumentAction.class.getName());
        registry.addCustomAction(scxmlNode, "http://www.onehippo.org/cms7/repository/scxml", "movedocument", MoveDocumentAction.class.getName());
        registry.addCustomAction(scxmlNode, "http://www.onehippo.org/cms7/repository/scxml", "renamedocument", RenameDocumentAction.class.getName());
        registry.setUp(scxmlConfigNode);

        HippoServiceRegistry.registerService(registry, SCXMLRegistry.class);
        HippoServiceRegistry.registerService(new RepositorySCXMLExecutorFactory(), SCXMLExecutorFactory.class);
    }

    @AfterClass
    public static void afterClass() throws Exception {
        HippoServiceRegistry.unregisterService(HippoServiceRegistry.getService(SCXMLExecutorFactory.class), SCXMLExecutorFactory.class);
        HippoServiceRegistry.unregisterService(HippoServiceRegistry.getService(SCXMLRegistry.class), SCXMLRegistry.class);
    }

    protected static String loadTestSCXML() throws Exception {
        return IOUtils.toString(TestDocumentWorkflowImpl.class.getResourceAsStream("test-document-workflow.scxml"));
    }

/*
    protected static String loadSCXML() throws Exception {
        InputStream is = null;

        try {
            is = TestDocumentWorkflowImpl.class.getResourceAsStream("/scxml-definitions.xml");
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);
            XPathExpression xpathExpr = XPathFactory.newInstance().newXPath()
                    .compile("//node[@name='reviewed-actions-workflow']/property[@name='hipposcxml:source']/value/text()");
            return StringUtils.trim((String) xpathExpr.evaluate(doc.getDocumentElement(), XPathConstants.STRING));
        } finally {
            IOUtils.closeQuietly(is);
        }
    }
*/

    protected MockNode addVariant(MockNode handle, String state) throws RepositoryException {
        MockNode variant = handle.addMockNode(handle.getName(), HippoStdPubWfNodeType.HIPPOSTDPUBWF_DOCUMENT);
        variant.setProperty(HippoStdNodeType.HIPPOSTD_STATE, state);
        return variant;
    }

    protected MockNode addRequest(MockNode handle, String type) throws RepositoryException {
        MockNode variant = handle.addMockNode(PublicationRequest.HIPPO_REQUEST, HippoNodeType.NT_REQUEST);
        variant.setProperty(PublicationRequest.HIPPOSTDPUBWF_TYPE, type);
        return variant;
    }

    protected Set<String> set(String... vargs) {
        Set<String> set = new TreeSet<>();
        Collections.addAll(set, vargs);
        return set;
    }

    protected Set<String> getCurrentStateIds(SCXMLWorkflowExecutor executor) {
        Set<TransitionTarget> targets = executor.getSCXMLExecutor().getCurrentStatus().getStates();

        if (targets.isEmpty()) {
            return Collections.emptySet();
        }

        Set<String> ids = new TreeSet<>();

        for (TransitionTarget target : targets) {
            ids.add(target.getId());
        }

        return ids;
    }

    protected void assertMatchingStateIds(SCXMLWorkflowExecutor executor, String ... ids) {
        Set<String> stateIds = getCurrentStateIds(executor);
        if (ids.length == stateIds.size()) {
            for (String id : ids) {
                if (!stateIds.contains(id))
                    Assert.fail("Current SCXML states "+stateIds+" not matching expected states "+set(ids)+"");
            }
            return;
        }
        Assert.fail("Current SCXML states ["+stateIds+"] not matching expected states "+set(ids)+"");
    }

    protected void assertContainsStateIds(SCXMLWorkflowExecutor executor, String ... ids) {
        Set<String> stateIds = getCurrentStateIds(executor);
        if (ids.length <= stateIds.size()) {
            for (String id : ids) {
                if (!stateIds.contains(id))
                    Assert.fail("Current SCXML states "+stateIds+" not containing expected states "+set(ids)+"");
            }
            return;
        }
        Assert.fail("Current SCXML states ["+stateIds+"] not containing expected states "+set(ids)+"");
    }

    protected void assertContainsHint(Map<String, Serializable> hints, String hint, Object value) {
        if (!hints.containsKey(hint) || ! hints.get(hint).toString().equals(value.toString())) {
            Assert.fail("Current hints "+hints+" not containing expected hint ["+hint+"] with value ["+value+"]");
        }
    }

    protected void assertNotContainsHint(Map<String, Serializable> hints, String hint) {
        if (hints.containsKey(hint)) {
            Assert.fail("Current hints "+hints+" contains not expected hint ["+hint+"]");
        }
    }

    @SuppressWarnings("unchecked")
    protected void putWorkflowConfig(RepositoryMap workflowConfig, String key, String value) {
        workflowConfig.put(key,value);
    }

    @Test
    public void testStatusState() throws Exception {

        MockAccessManagedSession session = new MockAccessManagedSession(MockNode.root());
        MockWorkflowContext workflowContext = new MockWorkflowContext("testuser", session);
        RepositoryMap workflowConfig = workflowContext.getWorkflowConfiguration();
        DocumentWorkflowImpl wf = new DocumentWorkflowImpl();
        wf.setWorkflowContext(workflowContext);

        MockNode handleNode = MockNode.root().addMockNode("test", HippoNodeType.NT_HANDLE);
        MockNode draftVariant, unpublishedVariant, publishedVariant;

        draftVariant = addVariant(handleNode, HippoStdNodeType.DRAFT);
        unpublishedVariant = addVariant(handleNode, HippoStdNodeType.UNPUBLISHED);
        publishedVariant = addVariant(handleNode, HippoStdNodeType.PUBLISHED);

        putWorkflowConfig(workflowConfig, "workflow.supportedFeatures", DocumentWorkflow.SupportedFeatures.request.name());
        wf.setNode(unpublishedVariant);

        assertContainsStateIds(wf.getWorkflowExecutor(), "status");
        assertNotContainsHint(wf.hints(), "status");
        assertNotContainsHint(wf.hints(), "checkModified");

        putWorkflowConfig(workflowConfig, "workflow.supportedFeatures", DocumentWorkflow.SupportedFeatures.document.name());
        wf.setNode(unpublishedVariant);

        assertContainsHint(wf.hints(), "status", true);
        assertContainsHint(wf.hints(), "checkModified", false);

        wf.setNode(draftVariant);

        assertContainsHint(wf.hints(), "status", false);
        assertContainsHint(wf.hints(), "checkModified", true);

        wf.setNode(publishedVariant);

        assertContainsHint(wf.hints(), "status", false);
        assertContainsHint(wf.hints(), "checkModified", false);

        unpublishedVariant.remove();
        wf.setNode(publishedVariant);

        assertContainsHint(wf.hints(), "status", true);
        assertContainsHint(wf.hints(), "checkModified", false);

        wf.setNode(draftVariant);

        assertContainsHint(wf.hints(), "status", false);
        assertContainsHint(wf.hints(), "checkModified", false);

        publishedVariant.remove();
        wf.setNode(draftVariant);

        assertContainsHint(wf.hints(), "status", true);

        draftVariant.setProperty(HippoStdNodeType.HIPPOSTD_HOLDER, "testuser");
        wf.setNode(draftVariant);

        assertContainsHint(wf.hints(), "status", true);

        draftVariant.setProperty(HippoStdNodeType.HIPPOSTD_HOLDER, "otheruser");
        wf.setNode(draftVariant);

        assertContainsHint(wf.hints(), "status", false);

        unpublishedVariant = addVariant(handleNode, HippoStdNodeType.UNPUBLISHED);
        wf.setNode(unpublishedVariant);

        assertContainsHint(wf.hints(), "status", false);

        draftVariant.remove();
        wf.setNode(unpublishedVariant);

        assertContainsHint(wf.hints(), "status", true);
    }

        @Test
    public void testEditState() throws Exception {

        MockAccessManagedSession session = new MockAccessManagedSession(MockNode.root());
        MockWorkflowContext workflowContext = new MockWorkflowContext("testuser", session);
        RepositoryMap workflowConfig = workflowContext.getWorkflowConfiguration();
        DocumentWorkflowImpl wf = new DocumentWorkflowImpl();
        wf.setWorkflowContext(workflowContext);

        MockNode handleNode = MockNode.root().addMockNode("test", HippoNodeType.NT_HANDLE);
        MockNode draftVariant, unpublishedVariant, publishedVariant, rejectedRequest, publishRequest;

        draftVariant = addVariant(handleNode, HippoStdNodeType.DRAFT);
        putWorkflowConfig(workflowConfig, "workflow.supportedFeatures", DocumentWorkflow.SupportedFeatures.request.name());
        wf.setNode(draftVariant);

        assertContainsStateIds(wf.getWorkflowExecutor(), "no-edit");
        assertNotContainsHint(wf.hints(), "obtainEditableInstance");
        assertNotContainsHint(wf.hints(), "commitEditableInstance");
        assertNotContainsHint(wf.hints(), "disposeEditableInstance");
        assertNotContainsHint(wf.hints(), "unlock");

        workflowConfig.remove("workflow.supportedFeatures");
        wf.setNode(draftVariant);

        // test state not-editable
        assertContainsStateIds(wf.getWorkflowExecutor(), "editable");

        rejectedRequest = addRequest(handleNode, PublicationRequest.REJECTED);
        wf.setNode(rejectedRequest);

        assertContainsStateIds(wf.getWorkflowExecutor(), "no-edit");

        rejectedRequest.remove();
        wf.setNode(draftVariant);

        assertContainsStateIds(wf.getWorkflowExecutor(), "editable");

        publishRequest = addRequest(handleNode, PublicationRequest.PUBLISH);
        wf.setNode(draftVariant);

        assertContainsStateIds(wf.getWorkflowExecutor(), "not-editable");

        publishRequest.remove();

        // test state not-editing
        wf.setNode(draftVariant);

        assertContainsStateIds(wf.getWorkflowExecutor(), "editable");
        assertContainsHint(wf.hints(), "obtainEditableInstance", true);
        assertContainsHint(wf.hints(), "unlock", false);

        putWorkflowConfig(workflowConfig, "workflow.supportedFeatures", DocumentWorkflow.SupportedFeatures.document.name());
        wf.setNode(draftVariant);

        assertContainsHint(wf.hints(), "obtainEditableInstance", true);
        assertNotContainsHint(wf.hints(), "unlock");

        putWorkflowConfig(workflowConfig, "workflow.supportedFeatures", DocumentWorkflow.SupportedFeatures.unlock.name());
        wf.setNode(draftVariant);

        assertContainsHint(wf.hints(), "unlock", false);
        assertNotContainsHint(wf.hints(), "obtainEditableInstance");

        workflowConfig.remove("workflow.supportedFeatures");
        unpublishedVariant = addVariant(handleNode, HippoStdNodeType.UNPUBLISHED);
        wf.setNode(draftVariant);

        assertContainsHint(wf.hints(), "obtainEditableInstance", false);

        wf.setNode(unpublishedVariant);

        assertContainsHint(wf.hints(), "obtainEditableInstance", true);

        unpublishedVariant.remove();
        draftVariant.remove();

        publishedVariant = addVariant(handleNode, HippoStdNodeType.PUBLISHED);
        wf.setNode(publishedVariant);

        assertContainsHint(wf.hints(), "obtainEditableInstance", true);

        rejectedRequest = addRequest(handleNode, PublicationRequest.REJECTED);
        wf.setNode(publishedVariant);

        assertContainsHint(wf.hints(), "obtainEditableInstance", true);
        assertContainsStateIds(wf.getWorkflowExecutor(), "editable");

        wf.setNode(rejectedRequest);

        assertContainsStateIds(wf.getWorkflowExecutor(), "no-edit");

        // test state editing
        publishedVariant.remove();
        rejectedRequest.remove();

        putWorkflowConfig(workflowConfig, "workflow.supportedFeatures", DocumentWorkflow.SupportedFeatures.document.name());
        draftVariant = addVariant(handleNode, HippoStdNodeType.DRAFT);
        draftVariant.setProperty(HippoStdNodeType.HIPPOSTD_HOLDER, "testuser");
        wf.setNode(draftVariant);

        assertContainsStateIds(wf.getWorkflowExecutor(), "editing");
        assertContainsHint(wf.hints(), "obtainEditableInstance", true);
        assertContainsHint(wf.hints(), "commitEditableInstance", true);
        assertContainsHint(wf.hints(), "disposeEditableInstance", true);
        assertNotContainsHint(wf.hints(), "unlock");

        putWorkflowConfig(workflowConfig, "workflow.supportedFeatures", DocumentWorkflow.SupportedFeatures.unlock.name());
        wf.setNode(draftVariant);

        assertContainsStateIds(wf.getWorkflowExecutor(), "editing");
        assertNotContainsHint(wf.hints(), "obtainEditableInstance");
        assertNotContainsHint(wf.hints(), "commitEditableInstance");
        assertNotContainsHint(wf.hints(), "disposeEditableInstance");
        assertContainsHint(wf.hints(), "unlock", true);

        workflowConfig.remove("workflow.supportedFeatures");
        wf.setNode(draftVariant);

        assertContainsStateIds(wf.getWorkflowExecutor(), "editing");
        assertContainsHint(wf.hints(), "obtainEditableInstance", true);
        assertContainsHint(wf.hints(), "commitEditableInstance", true);
        assertContainsHint(wf.hints(), "disposeEditableInstance", true);
        assertContainsHint(wf.hints(), "unlock", true);
        assertNotContainsHint(wf.hints(), "inUseBy");

        draftVariant.setProperty(HippoStdNodeType.HIPPOSTD_HOLDER, "otheruser");
        session.setPermissions(draftVariant.getPath(), "hippo:admin", false);
        wf.setNode(draftVariant);

        assertContainsStateIds(wf.getWorkflowExecutor(), "editing");
        assertContainsHint(wf.hints(), "obtainEditableInstance", false);
        assertContainsHint(wf.hints(), "commitEditableInstance", false);
        assertContainsHint(wf.hints(), "disposeEditableInstance", false);
        assertContainsHint(wf.hints(), "unlock", false);
        assertContainsHint(wf.hints(), "inUseBy", "otheruser");

        session.setPermissions(draftVariant.getPath(), "hippo:admin", true);
        wf.setNode(draftVariant);

        assertContainsStateIds(wf.getWorkflowExecutor(), "editing");
        assertContainsHint(wf.hints(), "obtainEditableInstance", false);
        assertContainsHint(wf.hints(), "commitEditableInstance", false);
        assertContainsHint(wf.hints(), "disposeEditableInstance", false);
        assertContainsHint(wf.hints(), "unlock", true);
    }

    @Test
    public void testRequestState() throws Exception {
        MockAccessManagedSession session = new MockAccessManagedSession(MockNode.root());
        MockWorkflowContext workflowContext = new MockWorkflowContext("testuser", session);
        RepositoryMap workflowConfig = workflowContext.getWorkflowConfiguration();
        DocumentWorkflowImpl wf = new DocumentWorkflowImpl();
        wf.setWorkflowContext(workflowContext);

        MockNode handleNode = MockNode.root().addMockNode("test", HippoNodeType.NT_HANDLE);
        MockNode unpublishedVariant, rejectedRequest, publishRequest;

        // test state not-requested
        unpublishedVariant = addVariant(handleNode, HippoStdNodeType.UNPUBLISHED);
        wf.setNode(unpublishedVariant);

        assertContainsStateIds(wf.getWorkflowExecutor(), "not-requested");

        // test state requested
        publishRequest = addRequest(handleNode, PublicationRequest.PUBLISH);
        wf.setNode(unpublishedVariant);

        assertContainsStateIds(wf.getWorkflowExecutor(), "requested");

        rejectedRequest = addRequest(handleNode, PublicationRequest.REJECTED);
        wf.setNode(unpublishedVariant);

        assertContainsStateIds(wf.getWorkflowExecutor(), "requested");

        wf.setNode(rejectedRequest);

        assertContainsStateIds(wf.getWorkflowExecutor(), "request-rejected");

        session.setPermissions(publishRequest.getPath(), "hippo:editor", false);
        wf.setNode(publishRequest);

        assertContainsStateIds(wf.getWorkflowExecutor(), "requested");
        assertContainsHint(wf.hints(), "cancelRequest", false);
        assertNotContainsHint(wf.hints(), "acceptRequest");
        assertNotContainsHint(wf.hints(), "rejectRequest");

        putWorkflowConfig(workflowConfig, "workflow.supportedFeatures", DocumentWorkflow.SupportedFeatures.document.name());
        wf.setNode(publishRequest);

        assertContainsStateIds(wf.getWorkflowExecutor(), "requested");
        assertContainsHint(wf.hints(), "cancelRequest", false);
        assertNotContainsHint(wf.hints(), "acceptRequest");
        assertNotContainsHint(wf.hints(), "rejectRequest");

        putWorkflowConfig(workflowConfig, "workflow.supportedFeatures", DocumentWorkflow.SupportedFeatures.request.name());
        session.setPermissions(publishRequest.getPath(), "hippo:editor", true);

        wf.setNode(publishRequest);
        assertContainsStateIds(wf.getWorkflowExecutor(), "requested");
        assertContainsHint(wf.hints(), "cancelRequest", false);
        assertContainsHint(wf.hints(), "acceptRequest", true);
        assertContainsHint(wf.hints(), "rejectRequest", true);

        publishRequest.setProperty(PublicationRequest.HIPPOSTDPUBWF_USERNAME, "testuser");
        wf.setNode(publishRequest);

        assertContainsHint(wf.hints(), "cancelRequest", true);
        assertContainsHint(wf.hints(), "rejectRequest", false);

        publishRequest.setProperty(PublicationRequest.HIPPOSTDPUBWF_USERNAME, "otheruser");
        wf.setNode(publishRequest);

        assertContainsHint(wf.hints(), "cancelRequest", false);
        assertContainsHint(wf.hints(), "rejectRequest", true);

        session.setPermissions(publishRequest.getPath(), "hippo:editor", false);
        wf.setNode(publishRequest);

        assertContainsHint(wf.hints(), "cancelRequest", false);
        assertNotContainsHint(wf.hints(), "acceptRequest");
        assertNotContainsHint(wf.hints(), "rejectRequest");

        publishRequest.setProperty(PublicationRequest.HIPPOSTDPUBWF_USERNAME, "testuser");

        wf.setNode(publishRequest);

        assertContainsHint(wf.hints(), "cancelRequest", true);
        assertNotContainsHint(wf.hints(), "acceptRequest");
        assertNotContainsHint(wf.hints(), "rejectRequest");

        // test state request-rejected

        session.setPermissions(rejectedRequest.getPath(), "hippo:editor", false);
        wf.setNode(rejectedRequest);

        assertContainsStateIds(wf.getWorkflowExecutor(), "request-rejected");
        assertContainsHint(wf.hints(), "cancelRequest", false);
        assertContainsHint(wf.hints(), "acceptRequest", false);
        assertContainsHint(wf.hints(), "rejectRequest", false);

        putWorkflowConfig(workflowConfig, "workflow.supportedFeatures", DocumentWorkflow.SupportedFeatures.document.name());
        wf.setNode(rejectedRequest);

        assertContainsStateIds(wf.getWorkflowExecutor(), "request-rejected");

        workflowConfig.remove("workflow.supportedFeatures");
        rejectedRequest.setProperty(PublicationRequest.HIPPOSTDPUBWF_USERNAME, "testuser");
        wf.setNode(rejectedRequest);

        assertContainsHint(wf.hints(), "cancelRequest", true);
        assertContainsHint(wf.hints(), "acceptRequest", false);
        assertContainsHint(wf.hints(), "rejectRequest", false);

        rejectedRequest.setProperty(PublicationRequest.HIPPOSTDPUBWF_USERNAME, "otheruser");
        wf.setNode(rejectedRequest);

        assertContainsHint(wf.hints(), "cancelRequest", false);
        assertContainsHint(wf.hints(), "acceptRequest", false);
        assertContainsHint(wf.hints(), "rejectRequest", false);

        session.setPermissions(rejectedRequest.getPath(), "hippo:editor", true);
        wf.setNode(rejectedRequest);

        assertContainsHint(wf.hints(), "cancelRequest", true);
        assertContainsHint(wf.hints(), "acceptRequest", false);
        assertContainsHint(wf.hints(), "rejectRequest", false);

        publishRequest.getProperty(PublicationRequest.HIPPOSTDPUBWF_USERNAME).remove();
        wf.setNode(rejectedRequest);

        assertContainsHint(wf.hints(), "cancelRequest", true);
        assertContainsHint(wf.hints(), "acceptRequest", false);
        assertContainsHint(wf.hints(), "rejectRequest", false);

        session.setPermissions(rejectedRequest.getPath(), "hippo:editor", false);
        wf.setNode(rejectedRequest);

        assertContainsHint(wf.hints(), "cancelRequest", false);
        assertContainsHint(wf.hints(), "acceptRequest", false);
        assertContainsHint(wf.hints(), "rejectRequest", false);
    }

    @Test
    public void testPublishState() throws Exception {
        MockAccessManagedSession session = new MockAccessManagedSession(MockNode.root());
        MockWorkflowContext workflowContext = new MockWorkflowContext("testuser", session);
        RepositoryMap workflowConfig = workflowContext.getWorkflowConfiguration();
        DocumentWorkflowImpl wf = new DocumentWorkflowImpl();
        wf.setWorkflowContext(workflowContext);

        MockNode handleNode = MockNode.root().addMockNode("test", HippoNodeType.NT_HANDLE);
        MockNode draftVariant, unpublishedVariant, publishedVariant, publishRequest;

        unpublishedVariant = addVariant(handleNode, HippoStdNodeType.UNPUBLISHED);
        putWorkflowConfig(workflowConfig, "workflow.supportedFeatures", DocumentWorkflow.SupportedFeatures.request.name());
        wf.setNode(unpublishedVariant);

        assertContainsStateIds(wf.getWorkflowExecutor(), "no-publish");
        assertNotContainsHint(wf.hints(), "publish");

        workflowConfig.remove("workflow.supportedFeatures");
        wf.setNode(unpublishedVariant);

        assertContainsStateIds(wf.getWorkflowExecutor(), "publishable");
        assertContainsHint(wf.hints(), "publish", true);

        unpublishedVariant.remove();
        publishRequest = addRequest(handleNode, PublicationRequest.PUBLISH);
        wf.setNode(publishRequest);

        assertContainsStateIds(wf.getWorkflowExecutor(), "no-publish");
        assertNotContainsHint(wf.hints(), "publish");

        unpublishedVariant = addVariant(handleNode, HippoStdNodeType.UNPUBLISHED);
        wf.setNode(unpublishedVariant);

        assertContainsHint(wf.hints(), "publish", false);

        workflowContext.setUserIdentity("system");
        wf.setNode(unpublishedVariant);

        assertContainsHint(wf.hints(), "publish", true);

        workflowContext.setUserIdentity("testuser");
        publishRequest.remove();
        draftVariant = addVariant(handleNode, HippoStdNodeType.DRAFT);
        wf.setNode(draftVariant);

        assertContainsHint(wf.hints(), "publish", false);

        wf.setNode(unpublishedVariant);

        assertContainsHint(wf.hints(), "publish", true);

        draftVariant.setProperty(HippoStdNodeType.HIPPOSTD_HOLDER, "testuser");
        wf.setNode(unpublishedVariant);

        assertContainsStateIds(wf.getWorkflowExecutor(), "not-publishable");

        draftVariant.setProperty(HippoStdNodeType.HIPPOSTD_HOLDER, "otheruser");
        wf.setNode(unpublishedVariant);

        assertContainsStateIds(wf.getWorkflowExecutor(), "not-publishable");

        draftVariant.getProperty(HippoStdNodeType.HIPPOSTD_HOLDER).remove();

        wf.setNode(unpublishedVariant);

        assertContainsHint(wf.hints(), "publish", true);

        publishedVariant = addVariant(handleNode, HippoStdNodeType.PUBLISHED);
        wf.setNode(unpublishedVariant);

        assertContainsHint(wf.hints(), "publish", false);

        publishedVariant.setProperty(HippoNodeType.HIPPO_AVAILABILITY, new String[0]);
        wf.setNode(unpublishedVariant);

        assertContainsHint(wf.hints(), "publish", true);

        publishedVariant.getProperty(HippoNodeType.HIPPO_AVAILABILITY).remove();

        Calendar publishedModified = Calendar.getInstance();
        Calendar unpublishedModified = Calendar.getInstance();
        publishedModified.setTimeInMillis(unpublishedModified.getTimeInMillis()-1000);
        unpublishedVariant.setProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_LAST_MODIFIED_DATE, new MockValue(PropertyType.DATE, ISO8601.format(unpublishedModified)));
        publishedVariant.setProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_LAST_MODIFIED_DATE, new MockValue(PropertyType.DATE, ISO8601.format(publishedModified)));

        wf.setNode(unpublishedVariant);

        assertContainsHint(wf.hints(), "publish", true);

        publishedVariant.setProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_LAST_MODIFIED_DATE, new MockValue(PropertyType.DATE, ISO8601.format(unpublishedModified)));

        wf.setNode(unpublishedVariant);

        assertContainsHint(wf.hints(), "publish", false);
    }

    @Test
    public void testDePublishState() throws Exception {
        MockAccessManagedSession session = new MockAccessManagedSession(MockNode.root());
        MockWorkflowContext workflowContext = new MockWorkflowContext("testuser", session);
        RepositoryMap workflowConfig = workflowContext.getWorkflowConfiguration();
        DocumentWorkflowImpl wf = new DocumentWorkflowImpl();
        wf.setWorkflowContext(workflowContext);

        MockNode handleNode = MockNode.root().addMockNode("test", HippoNodeType.NT_HANDLE);
        MockNode draftVariant, unpublishedVariant, publishedVariant, publishRequest;

        publishedVariant = addVariant(handleNode, HippoStdNodeType.PUBLISHED);
        putWorkflowConfig(workflowConfig, "workflow.supportedFeatures", DocumentWorkflow.SupportedFeatures.request.name());
        wf.setNode(publishedVariant);

        assertContainsStateIds(wf.getWorkflowExecutor(), "no-depublish");
        assertNotContainsHint(wf.hints(), "depublish");

        workflowConfig.remove("workflow.supportedFeatures");
        wf.setNode(publishedVariant);

        // note: empty/no availability means allways available
        assertContainsStateIds(wf.getWorkflowExecutor(), "depublishable");
        assertContainsHint(wf.hints(), "depublish", true);

        publishedVariant.setProperty(HippoNodeType.HIPPO_AVAILABILITY, new String[]{"foo"});

        wf.setNode(publishedVariant);

        assertContainsStateIds(wf.getWorkflowExecutor(), "not-depublishable");
        assertContainsHint(wf.hints(), "depublish", false);

        publishedVariant.setProperty(HippoNodeType.HIPPO_AVAILABILITY, new String[]{"foo","live"});

        wf.setNode(publishedVariant);

        assertContainsStateIds(wf.getWorkflowExecutor(), "depublishable");
        assertContainsHint(wf.hints(), "depublish", true);

        draftVariant = addVariant(handleNode, HippoStdNodeType.DRAFT);
        draftVariant.setProperty(HippoStdNodeType.HIPPOSTD_HOLDER, "testuser");
        wf.setNode(publishedVariant);

        assertContainsStateIds(wf.getWorkflowExecutor(), "not-depublishable");

        draftVariant.setProperty(HippoStdNodeType.HIPPOSTD_HOLDER, "otheruser");
        wf.setNode(publishedVariant);

        assertContainsStateIds(wf.getWorkflowExecutor(), "not-depublishable");

        draftVariant.getProperty(HippoStdNodeType.HIPPOSTD_HOLDER).remove();
        publishRequest = addRequest(handleNode, PublicationRequest.PUBLISH);
        wf.setNode(publishRequest);

        assertContainsStateIds(wf.getWorkflowExecutor(), "no-depublish");

        workflowContext.setUserIdentity("system");
        wf.setNode(draftVariant);

        assertContainsStateIds(wf.getWorkflowExecutor(), "depublishable");
        assertContainsHint(wf.hints(), "depublish", true);

        workflowContext.setUserIdentity("testuser");

        wf.setNode(draftVariant);

        assertContainsStateIds(wf.getWorkflowExecutor(), "depublishable");
        assertContainsHint(wf.hints(), "depublish", false);

        publishRequest.remove();

        wf.setNode(draftVariant);

        assertContainsStateIds(wf.getWorkflowExecutor(), "depublishable");
        assertContainsHint(wf.hints(), "depublish", true);

        unpublishedVariant = addVariant(handleNode, HippoStdNodeType.UNPUBLISHED);

        wf.setNode(draftVariant);

        assertContainsStateIds(wf.getWorkflowExecutor(), "depublishable");
        assertContainsHint(wf.hints(), "depublish", false);

        wf.setNode(publishedVariant);

        assertContainsStateIds(wf.getWorkflowExecutor(), "depublishable");
        assertContainsHint(wf.hints(), "depublish", false);

        wf.setNode(unpublishedVariant);

        assertContainsStateIds(wf.getWorkflowExecutor(), "depublishable");
        assertContainsHint(wf.hints(), "depublish", true);

        unpublishedVariant.remove();
        wf.setNode(publishedVariant);

        assertContainsStateIds(wf.getWorkflowExecutor(), "depublishable");
        assertContainsHint(wf.hints(), "depublish", false);

        draftVariant.remove();
        wf.setNode(publishedVariant);

        assertContainsStateIds(wf.getWorkflowExecutor(), "depublishable");
        assertContainsHint(wf.hints(), "depublish", true);
    }

    @Test
    public void testVersioningState() throws Exception {
        MockAccessManagedSession session = new MockAccessManagedSession(MockNode.root());
        MockWorkflowContext workflowContext = new MockWorkflowContext("testuser", session);
        RepositoryMap workflowConfig = workflowContext.getWorkflowConfiguration();
        DocumentWorkflowImpl wf = new DocumentWorkflowImpl();
        wf.setWorkflowContext(workflowContext);

        MockNode handleNode = MockNode.root().addMockNode("test", HippoNodeType.NT_HANDLE);
        MockNode unpublishedVariant, frozenNode;

        unpublishedVariant = addVariant(handleNode, HippoStdNodeType.UNPUBLISHED);

        MockVersion versionNode = new MockVersion("1.0", JcrConstants.NT_VERSION);
        ((MockNode)unpublishedVariant.getParent()).addNode(versionNode);
        frozenNode = versionNode.addMockNode(JcrConstants.JCR_FROZEN_NODE, JcrConstants.NT_FROZEN_NODE);
        frozenNode.setProperty(JcrConstants.JCR_FROZEN_UUID, unpublishedVariant.getIdentifier());

        wf.setNode(unpublishedVariant);

        assertContainsStateIds(wf.getWorkflowExecutor(), "version");
        assertContainsHint(wf.hints(), "version", true);
        assertContainsHint(wf.hints(), "listVersions", true);
        assertContainsHint(wf.hints(), "restoreVersion", true);
        assertContainsHint(wf.hints(), "revertVersion", true);
        assertNotContainsHint(wf.hints(), "restoreVersionTo");

        putWorkflowConfig(workflowConfig, "workflow.supportedFeatures", DocumentWorkflow.SupportedFeatures.document.name());
        wf.setNode(unpublishedVariant);

        assertContainsStateIds(wf.getWorkflowExecutor(), "no-versioning");
        assertNotContainsHint(wf.hints(), "version");
        assertNotContainsHint(wf.hints(), "restoreVersionTo");

        wf.setNode(frozenNode);
        assertContainsStateIds(wf.getWorkflowExecutor(), "version");
        assertContainsHint(wf.hints(), "restoreVersionTo", true);
        assertNotContainsHint(wf.hints(), "version");
    }

    @Test
    public void testTerminateState() throws Exception {
        MockAccessManagedSession session = new MockAccessManagedSession(MockNode.root());
        MockWorkflowContext workflowContext = new MockWorkflowContext("testuser", session);
        RepositoryMap workflowConfig = workflowContext.getWorkflowConfiguration();
        DocumentWorkflowImpl wf = new DocumentWorkflowImpl();
        wf.setWorkflowContext(workflowContext);

        MockNode handleNode = MockNode.root().addMockNode("test", HippoNodeType.NT_HANDLE);
        MockNode draftVariant, unpublishedVariant, publishedVariant, publishRequest;

        unpublishedVariant = addVariant(handleNode, HippoStdNodeType.UNPUBLISHED);

        wf.setNode(unpublishedVariant);

        assertContainsStateIds(wf.getWorkflowExecutor(), "terminateable");
        assertContainsHint(wf.hints(), "delete", true);
        assertContainsHint(wf.hints(), "move", true);
        assertContainsHint(wf.hints(), "rename", true);

        draftVariant = addVariant(handleNode, HippoStdNodeType.DRAFT);
        publishedVariant = addVariant(handleNode, HippoStdNodeType.PUBLISHED);
        publishedVariant.setProperty(HippoNodeType.HIPPO_AVAILABILITY, new String[0]);

        wf.setNode(unpublishedVariant);

        assertContainsStateIds(wf.getWorkflowExecutor(), "terminateable");
        assertContainsHint(wf.hints(), "delete", true);
        assertContainsHint(wf.hints(), "move", true);
        assertContainsHint(wf.hints(), "rename", true);

        wf.setNode(publishedVariant);

        assertContainsStateIds(wf.getWorkflowExecutor(), "not-terminateable");
        assertContainsHint(wf.hints(), "delete", false);
        assertContainsHint(wf.hints(), "move", false);
        assertContainsHint(wf.hints(), "rename", false);

        session.setPermissions(publishedVariant.getPath(), "hippo:editor", false);
        wf.setNode(publishedVariant);

        assertContainsStateIds(wf.getWorkflowExecutor(), "not-terminateable");
        assertContainsHint(wf.hints(), "delete", false);
        assertNotContainsHint(wf.hints(), "move");
        assertNotContainsHint(wf.hints(), "rename");

        session.setPermissions(unpublishedVariant.getPath(), "hippo:editor", false);
        wf.setNode(unpublishedVariant);

        assertContainsStateIds(wf.getWorkflowExecutor(), "terminateable");
        assertContainsHint(wf.hints(), "delete", true);
        assertNotContainsHint(wf.hints(), "move");
        assertNotContainsHint(wf.hints(), "rename");

        putWorkflowConfig(workflowConfig, "workflow.supportedFeatures", DocumentWorkflow.SupportedFeatures.request.name());
        wf.setNode(unpublishedVariant);

        assertContainsStateIds(wf.getWorkflowExecutor(), "no-terminate");

        workflowConfig.remove("workflow.supportedFeatures");
        publishedVariant.setProperty(HippoNodeType.HIPPO_AVAILABILITY, new String[]{"live"});
        wf.setNode(unpublishedVariant);

        assertContainsStateIds(wf.getWorkflowExecutor(), "no-terminate");

        publishedVariant.setProperty(HippoNodeType.HIPPO_AVAILABILITY, new String[0]);
        draftVariant.setProperty(HippoStdNodeType.HIPPOSTD_HOLDER, "testuser");
        wf.setNode(unpublishedVariant);

        assertContainsStateIds(wf.getWorkflowExecutor(), "no-terminate");

        draftVariant.getProperty(HippoStdNodeType.HIPPOSTD_HOLDER).remove();
        publishRequest = addRequest(handleNode, PublicationRequest.PUBLISH);
        wf.setNode(unpublishedVariant);

        assertContainsStateIds(wf.getWorkflowExecutor(), "no-terminate");

        publishRequest.remove();
        unpublishedVariant.remove();

        wf.setNode(publishedVariant);

        assertContainsStateIds(wf.getWorkflowExecutor(), "terminateable");
        assertContainsHint(wf.hints(), "delete", true);

        wf.setNode(draftVariant);

        assertContainsStateIds(wf.getWorkflowExecutor(), "not-terminateable");
        assertContainsHint(wf.hints(), "delete", false);

        publishedVariant.remove();

        wf.setNode(draftVariant);

        assertContainsStateIds(wf.getWorkflowExecutor(), "terminateable");
        assertContainsHint(wf.hints(), "delete", true);
    }

    @Test
    public void testCopyState() throws Exception {
        MockAccessManagedSession session = new MockAccessManagedSession(MockNode.root());
        MockWorkflowContext workflowContext = new MockWorkflowContext("testuser", session);
        DocumentWorkflowImpl wf = new DocumentWorkflowImpl();
        wf.setWorkflowContext(workflowContext);

        MockNode handleNode = MockNode.root().addMockNode("test", HippoNodeType.NT_HANDLE);
        MockNode draftVariant, unpublishedVariant, publishedVariant, publishRequest;

        draftVariant = addVariant(handleNode, HippoStdNodeType.DRAFT);
        draftVariant.setProperty(HippoStdNodeType.HIPPOSTD_HOLDER, "otheruser");
        unpublishedVariant = addVariant(handleNode, HippoStdNodeType.UNPUBLISHED);
        publishedVariant = addVariant(handleNode, HippoStdNodeType.PUBLISHED);
        publishRequest = addRequest(handleNode, PublicationRequest.PUBLISH);

        wf.setNode(unpublishedVariant);

        assertContainsStateIds(wf.getWorkflowExecutor(), "copyable");
        assertContainsHint(wf.hints(), "copy", true);

        wf.setNode(publishedVariant);

        assertContainsStateIds(wf.getWorkflowExecutor(), "not-copyable");
        assertContainsHint(wf.hints(), "copy", false);

        unpublishedVariant.remove();
        wf.setNode(publishedVariant);

        assertContainsStateIds(wf.getWorkflowExecutor(), "copyable");
        assertContainsHint(wf.hints(), "copy", true);

        session.setPermissions(publishedVariant.getPath(), "hippo:editor", false);
        wf.setNode(publishedVariant);

        assertContainsStateIds(wf.getWorkflowExecutor(), "no-copy");
        assertNotContainsHint(wf.hints(), "copy");

        session.setPermissions(publishedVariant.getPath(), "hippo:editor", true);
        wf.setNode(publishRequest);

        assertContainsStateIds(wf.getWorkflowExecutor(), "no-copy");
        assertNotContainsHint(wf.hints(), "copy");

        wf.setNode(draftVariant);

        assertContainsStateIds(wf.getWorkflowExecutor(), "not-copyable");
        assertContainsHint(wf.hints(), "copy", false);

        wf.setNode(publishedVariant);

        assertContainsStateIds(wf.getWorkflowExecutor(), "copyable");
        assertContainsHint(wf.hints(), "copy", true);
    }

    @Test
    public void testNoState() throws Exception {
        MockAccessManagedSession session = new MockAccessManagedSession(MockNode.root());
        MockWorkflowContext workflowContext = new MockWorkflowContext("testuser", session);
        DocumentWorkflowImpl wf = new DocumentWorkflowImpl();
        wf.setWorkflowContext(workflowContext);

        MockNode handleNode = MockNode.root().addMockNode("test", HippoNodeType.NT_HANDLE);
        MockNode unpublishedVariant, frozenNode;

        unpublishedVariant = addVariant(handleNode, HippoStdNodeType.UNPUBLISHED);

        MockVersion versionNode = new MockVersion("1.0", JcrConstants.NT_VERSION);
        ((MockNode)unpublishedVariant.getParent()).addNode(versionNode);
        frozenNode = versionNode.addMockNode(JcrConstants.JCR_FROZEN_NODE, JcrConstants.NT_FROZEN_NODE);
        frozenNode.setProperty(JcrConstants.JCR_FROZEN_UUID, unpublishedVariant.getIdentifier());

        unpublishedVariant.remove();
        wf.setNode(frozenNode);

        assertMatchingStateIds(wf.getWorkflowExecutor(), "status", "no-edit", "no-request", "no-publish", "no-depublish", "no-versioning", "no-terminate", "no-copy");
        assertEquals(0, wf.hints().size());
    }
}