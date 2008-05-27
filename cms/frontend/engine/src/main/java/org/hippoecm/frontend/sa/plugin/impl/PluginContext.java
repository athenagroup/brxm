/*
 * Copyright 2008 Hippo
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
package org.hippoecm.frontend.sa.plugin.impl;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.wicket.IClusterable;
import org.hippoecm.frontend.sa.Home;
import org.hippoecm.frontend.sa.plugin.IPluginContext;
import org.hippoecm.frontend.sa.plugin.IPluginControl;
import org.hippoecm.frontend.sa.plugin.config.IClusterConfig;
import org.hippoecm.frontend.sa.plugin.config.IPluginConfig;
import org.hippoecm.frontend.sa.service.IServiceTracker;
import org.hippoecm.frontend.sa.service.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PluginContext implements IPluginContext, IClusterable {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(PluginContext.class);

    private Home page;
    private Map<String, List<IClusterable>> services;
    private Map<String, List<IServiceTracker>> listeners;
    private List<IPluginControl> children;
    private boolean initializing = true;
    private transient PluginManager manager = null;

    public PluginContext(Home page) {
        this.page = page;
        this.services = new HashMap<String, List<IClusterable>>();
        this.listeners = new HashMap<String, List<IServiceTracker>>();
        this.children = new LinkedList<IPluginControl>();
    }

    public IPluginControl start(IClusterConfig cluster) {
        PluginManager mgr = getManager();
        final IPluginControl[] controls = new IPluginControl[cluster.getPlugins().size()];
        int i = 0;
        for (IPluginConfig config : cluster.getPlugins()) {
            controls[i++] = mgr.start(config);
        }

        IPluginControl control = new IPluginControl() {
            private static final long serialVersionUID = 1L;

            public void stopPlugin() {
                for (IPluginControl control : controls) {
                    control.stopPlugin();
                }
            }
        };
        children.add(control);
        return control;
    }

    public <T extends IClusterable> T getService(String name) {
        return (T) getManager().getService(name);
    }

    public <T extends IClusterable> List<T> getServices(String name) {
        return getManager().getServices(name);
    }

    public <T extends IClusterable> ServiceReference<T> getReference(T service) {
        return getManager().getReference(service);
    }

    public void registerService(IClusterable service, String name) {
        List<IClusterable> list = services.get(name);
        if (list == null) {
            list = new LinkedList<IClusterable>();
            services.put(name, list);
        }
        list.add(service);
        getManager().registerService(service, name);
    }

    public void unregisterService(IClusterable service, String name) {
        List<IClusterable> list = services.get(name);
        if (list != null) {
            list.remove(service);
            getManager().unregisterService(service, name);
        } else {
            log.warn("unregistering service that wasn't registered.");
        }
    }

    public void registerTracker(IServiceTracker listener, String name) {
        List<IServiceTracker> list = listeners.get(name);
        if (list == null) {
            list = new LinkedList<IServiceTracker>();
            listeners.put(name, list);
        }
        list.add(listener);
        if (!initializing) {
            getManager().registerTracker(listener, name);
        }
    }

    public void unregisterTracker(IServiceTracker listener, String name) {
        List<IServiceTracker> list = listeners.get(name);
        if (list != null) {
            list.remove(listener);
            if (!initializing) {
                getManager().unregisterTracker(listener, name);
            }
        }
    }

    private PluginManager getManager() {
        if (manager == null) {
            manager = page.getPluginManager();
        }
        return manager;
    }

    void connect() {
        if (initializing) {
            PluginManager mgr = getManager();
            for (Map.Entry<String, List<IServiceTracker>> entry : listeners.entrySet()) {
                List<IServiceTracker> list = entry.getValue();
                for (IServiceTracker listener : list) {
                    mgr.registerTracker(listener, entry.getKey());
                }
            }
            initializing = false;
        } else {
            log.warn("context was already initialized");
        }
    }

    void stop() {
        PluginManager mgr = getManager();
        for (Map.Entry<String, List<IClusterable>> entry : services.entrySet()) {
            for (IClusterable service : entry.getValue()) {
                mgr.unregisterService(service, entry.getKey());
            }
        }
        services = new HashMap<String, List<IClusterable>>();
        for (Map.Entry<String, List<IServiceTracker>> entry : listeners.entrySet()) {
            for (IServiceTracker service : entry.getValue()) {
                mgr.unregisterTracker(service, entry.getKey());
            }
        }
        listeners = new HashMap<String, List<IServiceTracker>>();
        for (IPluginControl control : children) {
            control.stopPlugin();
        }
        children = new LinkedList<IPluginControl>();
    }

}
