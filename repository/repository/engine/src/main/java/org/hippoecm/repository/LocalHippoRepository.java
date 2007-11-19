/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.List;

import javax.jcr.AccessDeniedException;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.InvalidItemStateException;
import javax.jcr.InvalidSerializedDataException;
import javax.jcr.ItemExistsException;
import javax.jcr.LoginException;
import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.ValueFormatException;
import javax.jcr.Workspace;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.api.JackrabbitRepository;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.core.nodetype.EffectiveNodeType;
import org.apache.jackrabbit.core.nodetype.InvalidNodeTypeDefException;
import org.apache.jackrabbit.core.nodetype.NodeTypeDef;
import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.nodetype.compact.CompactNodeTypeDefReader;
import org.apache.jackrabbit.core.nodetype.compact.ParseException;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.jackrabbit.RepositoryImpl;
import org.hippoecm.repository.servicing.ServicingDecoratorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class LocalHippoRepository extends HippoRepository {
    /** SVN id placeholder */
    private final static String SVN_ID = "$Id$";

    /** Hippo Namespace */
    public final static String NAMESPACE_URI = "http://www.hippoecm.org/nt/1.0";

    /** Hippo Namespace prefix */
    public final static String NAMESPACE_PREFIX = "hippo";

    /** System property for overriding the repostiory path */
    public final static String SYSTEM_PATH_PROPERTY = "repo.path";

    /** System property for overriding the repostiory config file */
    public final static String SYSTEM_CONFIG_PROPERTY = "repo.config";

    /** System property for overriding the repostiory config file */
    public final static String SYSTEM_SERVLETCONFIG_PROPERTY = "repo.servletconfig";

    /** Default config file */
    public final static String DEFAULT_REPOSITORY_CONFIG = "repository.xml";

    protected final Logger log = LoggerFactory.getLogger(LocalHippoRepository.class);

    private JackrabbitRepository jackrabbitRepository = null;
    private ServicingDecoratorFactory hippoRepositoryFactory;

    /** Whether to generate a dump.xml file of the /configuration node at shutdown */
    private boolean dump = false;

    public LocalHippoRepository() throws RepositoryException {
        super();
        initialize();
    }

    public LocalHippoRepository(String location) throws RepositoryException {
        super(location);
        initialize();
    }

    protected String getLocation() {
        return super.getLocation();
    }

    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    /**
     * Construct the repository path, default getWorkingDirectory() is used.
     * If the system property repo.path can be used to override the default.
     * If repo.path starts with a '.' then the path is taken relative to the
     * getWorkingDirectory().
     * @return The absolute path to the file repository
     */
    private String getRepositoryPath() {
        String path = System.getProperty(LocalHippoRepository.SYSTEM_PATH_PROPERTY);

        if (path == null || "".equals(path)) {
            path = getWorkingDirectory();
        } else if (path.charAt(0) == '.') {
            // relative path
            path = getWorkingDirectory() + System.getProperty("file.separator") + path;
        } else if (path.startsWith("file://")) {
            path = path.substring(6);
        } else if (path.startsWith("file:/")) {
            path = path.substring(5);
        } else if (path.startsWith("file:")) {
            path = "/" + path.substring(5);
        }
        log.info("Using repository path: " + path);
        return path;
    }

    /**
     * If the "file://" protocol is used, the path MUST be absolute.
     * In all other cases the config file is used as a class resource.
     * @return InputStream to the repository config
     * @throws RepositoryException 
     */
    private InputStream getRepositoryConfigAsStream() throws RepositoryException {
        // get config from system prop
        String configName = System.getProperty(SYSTEM_CONFIG_PROPERTY);

        // if not set try to use the servletconfig
        if (configName == null || "".equals(configName)) {
            configName = System.getProperty(SYSTEM_SERVLETCONFIG_PROPERTY);
        }

        // if still not set use default
        if (configName == null || "".equals(configName)) {
            log.info("Using default repository config: " + DEFAULT_REPOSITORY_CONFIG);
            return getClass().getResourceAsStream(DEFAULT_REPOSITORY_CONFIG);
        }

        // resource
        if (!configName.startsWith("file:")) {
            log.info("Using resource repository config: " + configName);
            return getClass().getResourceAsStream(configName);
        }

        // parse file name
        if (configName.startsWith("file://")) {
            configName = configName.substring(6);
        } else if (configName.startsWith("file:/")) {
            configName = configName.substring(5);
        } else if (configName.startsWith("file:")) {
            configName = "/" + configName.substring(5);
        }
        log.info("Using file repository config: file:/" + configName);

        // get the bufferedinputstream
        File configFile = new File(configName);
        try {
            FileInputStream fis = new FileInputStream(configFile);
            return new BufferedInputStream(fis);
        } catch (FileNotFoundException e) {
            throw new RepositoryException("Repository config not found: file:/" + configName);
        }
    }

    private void initialize() throws RepositoryException {
        jackrabbitRepository = RepositoryImpl.create(RepositoryConfig.create(getRepositoryConfigAsStream(),
                getRepositoryPath()));
        repository = jackrabbitRepository;

        String result = repository.getDescriptor("OPTION_NODE_TYPE_REG_SUPPORTED");
        log.info("Node type registration support: " + (result != null ? result : "no"));

        hippoRepositoryFactory = new ServicingDecoratorFactory();
        repository = hippoRepositoryFactory.getRepositoryDecorator(repository);

        try {
            // get the current root/system session for the default workspace
            Session rootSession =  ((RepositoryImpl)jackrabbitRepository).getRootSession(null);
            Workspace workspace = rootSession.getWorkspace();
            NamespaceRegistry nsreg = workspace.getNamespaceRegistry();

            try {
                initializeNamespace(nsreg, NAMESPACE_PREFIX, NAMESPACE_URI);
            } catch (UnsupportedRepositoryOperationException ex) {
                throw new RepositoryException("Could not initialize repository with hippo namespace", ex);
            } catch (AccessDeniedException ex) {
                throw new RepositoryException("Could not initialize repository with hippo namespace", ex);
            }

            try {
                String cndName = "repository.cnd";
                log.info("Initializing nodetypes from: " + cndName);
                initializeNodetypes(workspace, getClass().getResourceAsStream(cndName), cndName);
                rootSession.save();
            } catch (ConstraintViolationException ex) {
                throw new RepositoryException("Could not initialize repository with hippo node types", ex);
            } catch (InvalidItemStateException ex) {
                throw new RepositoryException("Could not initialize repository with hippo node types", ex);
            } catch (ItemExistsException ex) {
                throw new RepositoryException("Could not initialize repository with hippo node types", ex);
            } catch (LockException ex) {
                throw new RepositoryException("Could not initialize repository with hippo node types", ex);
            } catch (NoSuchNodeTypeException ex) {
                throw new RepositoryException("Could not initialize repository with hippo node types", ex);
            } catch (ParseException ex) {
                throw new RepositoryException("Could not initialize repository with hippo node types", ex);
            } catch (VersionException ex) {
                throw new RepositoryException("Could not initialize repository with hippo node types", ex);
            } catch (AccessDeniedException ex) {
                throw new RepositoryException("Could not initialize repository with hippo node types", ex);
            }

            if (!rootSession.getRootNode().hasNode("hippo:configuration")) {
                log.info("Initializing configuration content");
                try {
                    InputStream configuration = getClass().getResourceAsStream("configuration.xml");
                    if (configuration != null) {
                        initializeNodecontent(rootSession, "/", configuration);
                        rootSession.save();
                    }
                } catch (AccessDeniedException ex) {
                    throw new RepositoryException("Could not initialize repository with configuration content", ex);
                } catch (ConstraintViolationException ex) {
                    throw new RepositoryException("Could not initialize repository with configuration content", ex);
                } catch (InvalidItemStateException ex) {
                    throw new RepositoryException("Could not initialize repository with configuration content", ex);
                } catch (ItemExistsException ex) {
                    throw new RepositoryException("Could not initialize repository with configuration content", ex);
                } catch (LockException ex) {
                    throw new RepositoryException("Could not initialize repository with configuration content", ex);
                } catch (NoSuchNodeTypeException ex) {
                    throw new RepositoryException("Could not initialize repository with configuration content", ex);
                } catch (VersionException ex) {
                    throw new RepositoryException("Could not initialize repository with configuration content", ex);
                }
            } else {
                log.info("Initial configuration content already present");
            }

            Node configurationNode = null;
            try {
                configurationNode = rootSession.getRootNode().getNode("hippo:configuration");
                if (configurationNode.hasNode("hippo:initialize")) {
                    Node initializationNode = null;
                    try {
                        initializationNode = configurationNode.getNode("hippo:initialize");
                    } catch (PathNotFoundException ex) {
                        assert (initializationNode != null); // cannot happen
                    }
                    log.info("Looking for custom initializations at " + initializationNode.getPath());
                    for (NodeIterator iter = initializationNode.getNodes(); iter.hasNext();) {
                        Node node = iter.nextNode();
                        log.info("Initializing configuration from " + node.getName());
                        try {
                            if (node.hasProperty(HippoNodeType.HIPPO_NAMESPACE)) {
                                if (log.isDebugEnabled())
                                    log.debug("Found namespace configuration");
                                Property p = null;
                                try {
                                    String namespace = node.getProperty(HippoNodeType.HIPPO_NAMESPACE).getString();
                                    log.info("Initializing namespace: " + namespace);
                                    initializeNamespace(nsreg, node.getName(), namespace);
                                    p.remove();
                                } catch (PathNotFoundException ex) {
                                    assert (p != null); // cannot happen
                                }
                            }
                            if (node.hasProperty(HippoNodeType.HIPPO_NODETYPES)) {
                                if (log.isDebugEnabled())
                                    log.debug("Found nodetypes configuration");
                                Property p = null;
                                try {
                                    String cndName = (p = node.getProperty(HippoNodeType.HIPPO_NODETYPES)).getString();
                                    InputStream cndStream = getClass().getResourceAsStream(cndName);
                                    if (cndStream == null) {
                                        log.warn("Cannot locate nodetype configuration '" + cndName + "', initialization skipped");                                        
                                    } else {
                                        log.info("Initializing nodetypes from: " + cndName);
                                        initializeNodetypes(workspace, cndStream, cndName);
                                        p.remove();
                                    }
                                } catch (PathNotFoundException ex) {
                                    assert (p != null); // cannot happen
                                }
                            }
                            if (node.hasProperty(HippoNodeType.HIPPO_CONTENT)) {
                                if (log.isDebugEnabled())
                                    log.debug("Found content configuration");
                                Property contentProperty = null;
                                Property rootProperty = null;
                                try {
                                    String contentName = (contentProperty = node.getProperty(HippoNodeType.HIPPO_CONTENT)).getString();                                    
                                    InputStream contentStream = getClass().getResourceAsStream(contentName);
                                    if (contentStream == null) {
                                        log.warn("Cannot locate content configuration '" + contentName + "', initialization skipped");
                                    } else {
                                        String root = "/";
                                        if (node.hasProperty(HippoNodeType.HIPPO_CONTENTROOT)) {
                                            root = (rootProperty = node.getProperty(HippoNodeType.HIPPO_CONTENTROOT)).getString();
                                            rootProperty.remove();
                                        }
                                        log.info("Initializing content from: " + contentName + " to " + root);
                                        initializeNodecontent(rootSession, root, contentStream);
                                        contentProperty.remove();
                                    }
                                } catch (PathNotFoundException ex) {
                                    assert (contentProperty != null); // cannot happen
                                    assert (rootProperty != null); // cannot happen
                                }
                            }
                            rootSession.save();
                        } catch (ParseException ex) {
                            log.error("configuration at specified by " + node.getPath() + " failed", ex);
                            rootSession.refresh(false);
                        } catch (AccessDeniedException ex) {
                            log.error("configuration at specified by " + node.getPath() + " failed", ex);
                            rootSession.refresh(false);
                        } catch (ConstraintViolationException ex) {
                            log.error("configuration at specified by " + node.getPath() + " failed", ex);
                            rootSession.refresh(false);
                        } catch (InvalidItemStateException ex) {
                            log.error("configuration at specified by " + node.getPath() + " failed", ex);
                            rootSession.refresh(false);
                        } catch (ItemExistsException ex) {
                            log.error("configuration at specified by " + node.getPath() + " failed", ex);
                            rootSession.refresh(false);
                        } catch (LockException ex) {
                            log.error("configuration at specified by " + node.getPath() + " failed", ex);
                            rootSession.refresh(false);
                        } catch (NoSuchNodeTypeException ex) {
                            log.error("configuration at specified by " + node.getPath() + " failed", ex);
                            rootSession.refresh(false);
                        } catch (UnsupportedRepositoryOperationException ex) {
                            log.error("configuration at specified by " + node.getPath() + " failed", ex);
                            rootSession.refresh(false);
                        } catch (ValueFormatException ex) {
                            log.error("configuration at specified by " + node.getPath() + " failed", ex);
                            rootSession.refresh(false);
                        } catch (VersionException ex) {
                            log.error("configuration at specified by " + node.getPath() + " failed", ex);
                            rootSession.refresh(false);
                        }
                    }
                }
            } catch (PathNotFoundException ex) {
                log.error("configuration node still not present");
            }
        } catch (LoginException ex) {
            log.error("no access to repository by repository itself", ex);
        }
    }

    private void initializeNamespace(NamespaceRegistry nsreg, String prefix, String uri)
            throws UnsupportedRepositoryOperationException, AccessDeniedException, RepositoryException {
        try {
            nsreg.registerNamespace(prefix, uri);
        } catch (NamespaceException ex) {
            if (ex.getMessage().endsWith("mapping already exists")) {
                log.debug("Namespace already exists: " + prefix + ":" + uri);
            }
            log.warn(ex.getMessage());
        }
    }

    private void initializeNodetypes(Workspace workspace, InputStream cndStream, String cndName) throws ParseException,
            RepositoryException {
        CompactNodeTypeDefReader cndReader = new CompactNodeTypeDefReader(new InputStreamReader(cndStream), cndName);
        List ntdList = cndReader.getNodeTypeDefs();
        NodeTypeManagerImpl ntmgr = (NodeTypeManagerImpl) workspace.getNodeTypeManager();
        NodeTypeRegistry ntreg = ntmgr.getNodeTypeRegistry();

        for (Iterator iter = ntdList.iterator(); iter.hasNext();) {
            NodeTypeDef ntd = (NodeTypeDef) iter.next();

            try {
                ntreg.unregisterNodeType(ntd.getName());
            } catch (NoSuchNodeTypeException ex) {
                // new type, ignore
            } catch (RepositoryException ex) {
                // kind of safe to ignore
            }

            try {
                EffectiveNodeType effnt = ntreg.registerNodeType(ntd);
                log.info("Added NodeType: " + ntd.getName().getLocalName());
            } catch (NamespaceException ex) {
                log.warn(ex.getMessage());
            } catch (InvalidNodeTypeDefException ex) {
                if (ex.getMessage().endsWith("already exists")) {
                    log.debug(ex.getMessage());
                } else {
                    log.warn(ex.getMessage());
                }
            } catch (RepositoryException ex) {
                if (!ex.getMessage().equals("not yet implemented")) {
                    log.warn(ex.getMessage());
                    ex.printStackTrace();
                }
            }
        }
    }

    private void initializeNodecontent(Session session, String absPath, InputStream istream) {
        try {
            String relpath = absPath.substring(1);
            if (relpath.length() > 0 && !session.getRootNode().hasNode(relpath)) {
                session.getRootNode().addNode(relpath);
            }
            session.importXML(absPath, istream, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
            ex.printStackTrace(System.err);
        } catch (PathNotFoundException ex) {
            System.err.println(ex.getMessage());
            ex.printStackTrace(System.err);
        } catch (ItemExistsException ex) {
            System.err.println(ex.getMessage());
            ex.printStackTrace(System.err);
        } catch (ConstraintViolationException ex) {
            System.err.println(ex.getMessage());
            ex.printStackTrace(System.err);
        } catch (VersionException ex) {
            System.err.println(ex.getMessage());
            ex.printStackTrace(System.err);
        } catch (InvalidSerializedDataException ex) {
            System.err.println(ex.getMessage());
            ex.printStackTrace(System.err);
        } catch (LockException ex) {
            System.err.println(ex.getMessage());
            ex.printStackTrace(System.err);
        } catch (RepositoryException ex) {
            System.err.println(ex.getMessage());
            ex.printStackTrace(System.err);
        }
    }

    public synchronized void close() {
        Session session = null;
        if (dump && repository != null) {
            try {
                session = login();
                java.io.OutputStream out = new java.io.FileOutputStream("dump.xml");
                session.exportSystemView("/configuration", out, false, false);
            } catch (IOException ex) {
                System.err.println(ex.getMessage());
                ex.printStackTrace(System.err);
            } catch (RepositoryException ex) {
                System.err.println(ex.getMessage());
                ex.printStackTrace(System.err);
            } finally {
                if (session != null) {
                    session.logout();
                }
            }
        }

        if (jackrabbitRepository != null) {
            try {
                jackrabbitRepository.shutdown();
                jackrabbitRepository = null;
            } catch (Exception ex) {
                // ignore
            }
        }
        repository = null;

        super.close();
    }
}
