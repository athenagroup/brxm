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
package org.hippoecm.frontend.editor.workflow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.util.tester.FormTester;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.editor.repository.TemplateEditorWorkflow;
import org.hippoecm.frontend.PluginTest;
import org.hippoecm.frontend.editor.layout.LayoutProviderPlugin;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.ModelReference;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JcrPluginConfig;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.service.ServiceTracker;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.util.Utilities;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class RemodelWorkflowPluginTest extends PluginTest {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id: ";

    public static final class MenuTesterPlugin extends RenderPlugin {
        private static final long serialVersionUID = 1L;

        public MenuTesterPlugin(IPluginContext context, IPluginConfig config) {
            super(context, config);

            context.registerTracker(new ServiceTracker<IRenderService>(IRenderService.class) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onServiceAdded(IRenderService service, String name) {
                    addOrReplace(new MenuTester("menu", (MarkupContainer) service.getComponent()));
                    redraw();
                }

            }, config.getString("actions"));
        }
    }

    final static String[] content = {
        "/test", "nt:unstructured",
            "/test/menu", "frontend:plugin",
                "plugin.class", MenuTesterPlugin.class.getName(),
                "wicket.id", "service.root",
                "actions", "service.actions",
            "/test/layouts", "frontend:plugin",
                "plugin.class", LayoutProviderPlugin.class.getName(),
            "/test/plugin", "frontend:plugin",
                "plugin.class", RemodelWorkflowPlugin.class.getName(),
                "wicket.id", "service.actions",
                "wicket.model", "service.model",
    };

    IPluginConfig config;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp(true);
        build(session, content);

        start(new JcrPluginConfig(new JcrNodeModel("/test/layouts")));
        start(new JcrPluginConfig(new JcrNodeModel("/test/menu")));

        WorkflowManager wflMgr = ((HippoWorkspace) session.getWorkspace()).getWorkflowManager();

        Node nsNode = session.getRootNode().getNode("hippo:namespaces");
        TemplateEditorWorkflow nsWfl = (TemplateEditorWorkflow) wflMgr.getWorkflow("test", nsNode);
        nsWfl.createNamespace("testns", "http://example.org/test/0.0");

        Node documentNode = session.getRootNode().getNode("hippo:namespaces/testns");
        Utilities.dump(documentNode);
        String category = "test";
        WorkflowDescriptor descriptor = wflMgr.getWorkflowDescriptor(category, documentNode);
        WorkflowDescriptorModel pluginModel = new WorkflowDescriptorModel(descriptor, category, documentNode);

        ModelReference ref = new ModelReference("service.model", pluginModel);
        ref.init(context);

        config = new JcrPluginConfig(new JcrNodeModel("/test/plugin"));
    }

    @After
    @Override
    public void teardown() throws Exception {
        root.getNode("hippo:namespaces/testns").remove();
        session.save();
        super.teardown();
    }

    @Test
    public void createDocumentTypeTest() {
        start(config);
        refreshPage();

        // "new document type"
        tester.clickLink("root:menu:1:link");
        //        printComponents(System.out);

        // "test-type"
        FormTester formTest = tester.newFormTester("dialog:content:form");
        formTest.setValue("view:name:widget", "testtype");
        formTest.selectMultiple("view:checkgroup", new int[] { 0 });

        // "next"
        formTest.submit("buttons:next");

        // "select layout"
        tester.clickLink("dialog:content:form:view:layouts:0:link");
        formTest = tester.newFormTester("dialog:content:form");
        formTest.submit("buttons:finish");

        JcrNodeModel nsNode = new JcrNodeModel("/hippo:namespaces/testns/testtype");
        assertTrue(nsNode.getItemModel().exists());

        nsNode = new JcrNodeModel("/hippo:namespaces/testns/testtype/editor:templates/_default_");
        assertTrue(nsNode.getItemModel().exists());
    }

    @Test
    public void createCompoundTypeTest() {
        start(config);
        refreshPage();

        // "new document type"
        tester.clickLink("root:menu:2:link");
//        printComponents(System.out);

        // "test-type"
        FormTester formTest = tester.newFormTester("dialog:content:form");
        formTest.setValue("view:name:widget", "testtype");

        // "next"
        formTest.submit("buttons:next");

        // "select layout"
        tester.clickLink("dialog:content:form:view:layouts:0:link");
        formTest = tester.newFormTester("dialog:content:form");
        formTest.submit("buttons:finish");

        JcrNodeModel nsNode = new JcrNodeModel("/hippo:namespaces/testns/testtype");
        assertTrue(nsNode.getItemModel().exists());

        nsNode = new JcrNodeModel("/hippo:namespaces/testns/testtype/editor:templates/_default_");
        assertTrue(nsNode.getItemModel().exists());
    }

    @Test
    public void remodelTest() throws Exception {
        start(config);
        refreshPage();

        // "new document type"
        tester.clickLink("root:menu:3:link");
        // "yes, I know what I'm doing"
        tester.clickLink("dialog:content:form:wizard:form:buttons:yes");
        tester.executeBehavior((AbstractAjaxBehavior) home.get("dialog:content:form:wizard:form:view:progress")
                .getBehaviors().get(0));

        //        printComponents(System.out);
        session = ((UserSession) org.apache.wicket.Session.get()).getJcrSession();
        NamespaceRegistry nsReg = session.getWorkspace().getNamespaceRegistry();
        assertEquals("http://example.org/test/0.1", nsReg.getURI("testns"));
    }

}
