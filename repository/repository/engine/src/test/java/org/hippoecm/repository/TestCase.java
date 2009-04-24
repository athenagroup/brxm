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
package org.hippoecm.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.PropertyDefinition;

import org.hippoecm.repository.api.HippoWorkspace;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

/**
 * Abstract base class for writing tests against repository.
 *
 * Your unit test should follow the following pattern:
 *
        <code>
        public class SampleTest extends org.hippoecm.repository.TestCase {
            public void setUp() throws Exception {
                super.setUp();
                // your code here
            }
            public void tearDown() throws Exception {
                // your code here
                super.tearDown();
            }
        }
        </code>
 */
public abstract class TestCase
    // used to extends junit.framework.TestCase
{
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    protected static final String SYSTEMUSER_ID = "admin";
    protected static final char[] SYSTEMUSER_PASSWORD = "admin".toCharArray();

    protected static HippoRepository external = null;
    protected HippoRepository server = null;
    protected Session session = null;

    public TestCase() {
    }

    static private void delete(File path) {
        if(path.exists()) {
            if(path.isDirectory()) {
                File[] files = path.listFiles();
                for(int i=0; i<files.length; i++)
                    delete(files[i]);
            }
            path.delete();
        }
    }

    static private void clear() {
        String[] files = new String[] { ".lock", "repository", "version", "workspaces" };
        for(int i=0; i<files.length; i++) {
            File file = new File(files[i]);
            delete(file);
        }
    }

    static public void setRepository(HippoRepository repository) {
        external = repository;
    }

    @BeforeClass public static void setUpClass() throws Exception {
    }

    protected static void setUpClass(boolean clearRepository) throws Exception {
        if(clearRepository)
            clear();
    }

    @AfterClass public static void tearDownClass() throws Exception {
        tearDownClass(false);
    }

    public static void tearDownClass(boolean clearRepository) throws Exception {
        if(clearRepository)
            clear();
    }

    @Before
    public void setUp() throws Exception {
        setUp(false);
    }

    protected void setUp(boolean clearRepository) throws Exception {
        if(clearRepository) {
            clear();
        }
        if (external != null) {
            server = external;
        } else {
            server = HippoRepositoryFactory.getHippoRepository();
        }
        session = server.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);
        while (session.getRootNode().hasNode("test")) {
            session.getRootNode().getNode("test").remove();
            session.save();
            session.refresh(false);
        }
    }
    
    @After
    public void tearDown() throws Exception {
        tearDown(false);
    }

    public void tearDown(boolean clearRepository) throws Exception {
        if (session != null) {
            session.refresh(false);
            while (session.getRootNode().hasNode("test")) {
                session.getRootNode().getNode("test").remove();
                session.save();
            }
            session.logout();
            session = null;
        }
        if (external == null && server != null) {
            server.close();
            server = null;
        }
        if(clearRepository) {
            clear();
        }
    }

    protected void build(Session session, String[] contents) throws RepositoryException {
        Node node = null;
        for (int i=0; i<contents.length; i+=2) {
            if (contents[i].startsWith("/")) {
                String path = contents[i].substring(1);
                node = session.getRootNode();
                if (path.contains("/")) {
                    node = node.getNode(path.substring(0,path.lastIndexOf("/")));
                    path = path.substring(path.lastIndexOf("/")+1);
                }
                node = node.addNode(path, contents[i+1]);
            } else {
                PropertyDefinition propDef = null;
                PropertyDefinition[] propDefs = node.getPrimaryNodeType().getPropertyDefinitions();
                for (int propidx=0; propidx<propDefs.length; propidx++)
                    if (propDefs[propidx].getName().equals(contents[i])) {
                        propDef = propDefs[propidx];
                        break;
                    }
                if ("jcr:mixinTypes".equals(contents[i])) {
                    node.addMixin(contents[i+1]);
                } else {
                    if (propDef != null && propDef.isMultiple()) {
                        Value[] values;
                        if (node.hasProperty(contents[i])) {
                            values = node.getProperty(contents[i]).getValues();
                            Value[] newValues = new Value[values.length+1];
                            System.arraycopy(values,0,newValues,0,values.length);
                            values = newValues;
                        } else {
                            if (contents[i+1] != null)
                                values = new Value[1];
                            else
                                values = new Value[0];
                        }
                        if (values.length > 0) {
                            if (propDef.getRequiredType() == PropertyType.REFERENCE) {
                                String uuid = session.getRootNode().getNode(contents[i+1]).getUUID();
                                values[values.length-1] = session.getValueFactory().createValue(uuid, PropertyType.REFERENCE);
                            } else {
                                values[values.length-1] = session.getValueFactory().createValue(contents[i+1]);
                            }
                        }
                        node.setProperty(contents[i], values);
                    } else {
                        if (propDef != null && propDef.getRequiredType() == PropertyType.REFERENCE) {
                            node.setProperty(contents[i], session.getValueFactory().createValue(session.getRootNode().
                                                         getNode(contents[i+1].substring(1)).getUUID(), PropertyType.REFERENCE));
                        } else if ("hippo:docbase".equals(contents[i])) {
                            String docbase;
                            if(contents[i+1].substring(1).equals("")) {
                                docbase = session.getRootNode().getUUID();
                            } else {
                                docbase = session.getRootNode().getNode(contents[i+1].substring(1)).getUUID();
                            }
                            node.setProperty(contents[i], session.getValueFactory().createValue(docbase), PropertyType.STRING);
                        } else {
                            node.setProperty(contents[i], contents[i+1]);
                        }
                    }
                }
            }
        }
    }

    protected Node traverse(Session session, String path) throws RepositoryException {
        if (path.startsWith("/"))
            path = path.substring(1);
        return ((HippoWorkspace)session.getWorkspace()).getHierarchyResolver().getNode(session.getRootNode(), path);
    }
}
