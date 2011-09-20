package org.onehippo.cms7.channelmanager.templatecomposer.iframe;/*
 *  Copyright 2011 Hippo.
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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import com.gargoylesoftware.htmlunit.AjaxController;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebWindow;
import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.javascript.host.Window;
import com.google.gson.Gson;

import org.junit.After;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.DefaultServlet;
import org.mortbay.thread.QueuedThreadPool;
import org.onehippo.cms7.channelmanager.templatecomposer.GlobalBundle;
import org.onehippo.cms7.channelmanager.templatecomposer.PageEditor;
import org.onehippo.cms7.jquery.JQueryBundle;
import org.w3c.dom.Text;

import net.sourceforge.htmlunit.corejs.javascript.BaseFunction;
import net.sourceforge.htmlunit.corejs.javascript.Function;
import net.sourceforge.htmlunit.corejs.javascript.Scriptable;
import net.sourceforge.htmlunit.corejs.javascript.ScriptableObject;

abstract public class AbstractChannelManagerTest {
    public static final String LISTEN_HOST = "localhost";
    public static final int LISTEN_PORT = 8888;

    public class Message {
        public String messageTag;
        public Object messagePayload;

        public Message(final String messageTag, final Object messagePayload) {
            this.messageTag = messageTag;
            this.messagePayload = messagePayload;
        }
    }

    Server server;
    protected HtmlPage page;
    private List<Message> messagesSend = new ArrayList<Message>();

    public void setUp(String name) throws Exception {
        server = new Server();

        QueuedThreadPool pool = new QueuedThreadPool();
        pool.setMinThreads(8);
        server.setThreadPool(pool);

        SelectChannelConnector connector = new SelectChannelConnector();
        connector.setHost(LISTEN_HOST);
        connector.setPort(LISTEN_PORT);
        server.setConnectors(new Connector[]{connector});

        Context root = new Context(server, "/", Context.SESSIONS);
        root.setResourceBase(".");
        root.addServlet(DefaultServlet.class, "/*");
        root.getSessionHandler().getSessionManager().setSessionURL("none");

        server.start();

        WebClient client = new WebClient(BrowserVersion.FIREFOX_3);
        client.setAjaxController(new AjaxController() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean processSynchron(final HtmlPage page, final WebRequest request, final boolean async) {
                return true;
            }
        });
        WebWindow testWindow = client.openWindow(new URL("http://localhost:" + LISTEN_PORT + "/" + name), LISTEN_HOST);
        startConsole(client);
        page = (HtmlPage) testWindow.getEnclosedPage();
        Window window = (Window) client.getCurrentWindow().getScriptObject();
        window.initialize(page);
    }

    @After
    public void tearDown() throws Exception {
        if (server != null) {
            server.stop();
        }
        server = null;
    }

    void startConsole(WebClient client) {
        client.initializeEmptyWindow(client.getCurrentWindow());
        Window window = (Window) client.getCurrentWindow().getScriptObject();

        Scriptable console = (Scriptable) window.get("console");
        if (console == null) {
            net.sourceforge.htmlunit.corejs.javascript.Context context = net.sourceforge.htmlunit.corejs.javascript.Context.enter();
            console = context.newObject(window, "Object");
            ScriptableObject.putProperty(window, "console", console);
        }

        final Function jsxLog = new BaseFunction() {
            private static final long serialVersionUID = -2445994102698852899L;

            @Override
            public Object call(net.sourceforge.htmlunit.corejs.javascript.Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                if (args.length > 0 && args[0] instanceof String) {
                    System.out.println((String) args[0]);
                }
                return null;
            }
        };
        ScriptableObject.putProperty(console, "log", jsxLog);

        final Function jsxError = new BaseFunction() {
            private static final long serialVersionUID = -2445994102698852899L;

            @Override
            public Object call(net.sourceforge.htmlunit.corejs.javascript.Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                if (args.length > 0 && args[0] instanceof String) {
                    System.err.println((String) args[0]);
                }
                return null;
            }
        };
        ScriptableObject.putProperty(console, "error", jsxError);

        final Function jsxWarn = new BaseFunction() {
            private static final long serialVersionUID = -2445994102698852899L;

            @Override
            public Object call(net.sourceforge.htmlunit.corejs.javascript.Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                if (args.length > 0 && args[0] instanceof String) {
                    System.err.println((String) args[0]);
                }
                return null;
            }
        };
        ScriptableObject.putProperty(console, "warn", jsxWarn);
    }

    protected String eval(String objectIdentifier) {
        return (String) page.executeJavaScript(objectIdentifier).getJavaScriptResult();
    }

    protected boolean isMetaDataConsumed(final HtmlElement containerDiv) {
        boolean metaDataConsumed = true;
        DomNode tmp = containerDiv;
        while ((tmp = tmp.getPreviousSibling()) != null) {
            if (tmp.getNodeType() == 8) {
                metaDataConsumed = false;
            }
        }
        return metaDataConsumed;
    }

    protected void initializeIFrameHead() throws IOException {
        injectJavascript(InitializationTest.class, "initMiFrameMessageMock.js");

        injectJavascript(JQueryBundle.class, JQueryBundle.JQUERY_CORE);
        injectJavascript(JQueryBundle.class, JQueryBundle.JQUERY_CLASS_PLUGIN);
        injectJavascript(JQueryBundle.class, JQueryBundle.JQUERY_NAMESPACE_PLUGIN);
        injectJavascript(JQueryBundle.class, JQueryBundle.JQUERY_UI);

        injectJavascript(GlobalBundle.class, GlobalBundle.GLOBALS);
        injectJavascript(IFrameBundle.class, IFrameBundle.UTIL);
        injectJavascript(IFrameBundle.class, IFrameBundle.SURFANDEDIT);
        injectJavascript(IFrameBundle.class, IFrameBundle.MANAGER);
        injectJavascript(IFrameBundle.class, IFrameBundle.FACTORY);
        injectJavascript(IFrameBundle.class, IFrameBundle.WIDGETS);
        injectJavascript(IFrameBundle.class, IFrameBundle.MAIN);

        Window window = (Window) page.getWebClient().getCurrentWindow().getScriptObject();
        final Function oldFunction = (Function) window.get("sendMessage");
        ScriptableObject.putProperty(window, "sendMessage", new BaseFunction() {
            @Override
            public Object call(final net.sourceforge.htmlunit.corejs.javascript.Context cx, final Scriptable scope, final Scriptable thisObj, final Object[] args) {
                AbstractChannelManagerTest.this.messagesSend.add(new Message((String)args[1], args[0]));
                return oldFunction.call(cx, scope, thisObj, args);
            }
        });
    }

    protected void initializeTemplateComposer(final Boolean debug, final Boolean previewMode) {
        ResourceBundle resourceBundle = ResourceBundle.getBundle(PageEditor.class.getName());
        final Map<String, String> resourcesMap = new HashMap<String, String>();
        for (String key : resourceBundle.keySet()) {
            resourcesMap.put(key, resourceBundle.getString(key));
        }

        Map<String, Object> argument = new HashMap<String, Object>();
        argument.put("debug", debug);
        argument.put("previewMode", previewMode);
        argument.put("resources", resourcesMap);

        Gson gson = new Gson();
        String message = gson.toJson(argument);

        page.executeJavaScript("sendMessage(" + message + ", 'init');");
    }

    protected void injectJavascript(Class<?> clazz, String resource) throws IOException {
        final InputStream inputStream = clazz.getResourceAsStream(resource);

        Reader resourceReader = new InputStreamReader(inputStream);
        StringBuilder javascript = new StringBuilder();
        int buffer = 0;
        try {
            while ((buffer = resourceReader.read()) != -1) {
                javascript.append((char) buffer);
            }
            final List<HtmlElement> head = page.getElementsByTagName("head");
            final HtmlElement script = page.createElement("script");
            script.setAttribute("type", "text/javascript");
            final Text textNode = page.createTextNode(javascript.toString());
            script.appendChild(textNode);
            head.get(0).appendChild(script);
        } finally {
            resourceReader.close();
        }
    }

    public boolean isMessageSend(final String message) {
        for (Message messageObject : this.messagesSend) {
            if (message.equals(messageObject.messageTag)) {
                return true;
            }
        }
        return false;
    }

    public void clearMessagesSend() {
        this.messagesSend.clear();
    }

    public List<Message> getMessagesSend() {
        return this.messagesSend;
    }

    public List<Message> getMessages(String message) {
        List<Message> messages = new ArrayList<Message>();
        for (Message messageObject : this.messagesSend) {
            if (message.equals(messageObject.messageTag)) {
                messages.add(messageObject);
            }
        }
        return messages;
    }

}
