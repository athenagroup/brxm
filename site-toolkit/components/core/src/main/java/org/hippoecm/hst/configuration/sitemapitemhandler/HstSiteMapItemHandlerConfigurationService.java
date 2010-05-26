/*
 *  Copyright 2010 Hippo.
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
package org.hippoecm.hst.configuration.sitemapitemhandler;

import java.util.Map;

import javax.jcr.Node;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.sitemapitemhandlers.HstSiteMapItemHandlerConfiguration;
import org.hippoecm.hst.service.AbstractJCRService;
import org.hippoecm.hst.service.Service;
import org.hippoecm.hst.service.ServiceException;

public class HstSiteMapItemHandlerConfigurationService extends AbstractJCRService implements HstSiteMapItemHandlerConfiguration, Service {

    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private String sitemapItemHandlerClassName;
    private Map<String, Object> properties;
    private long createdTime;
    
    public HstSiteMapItemHandlerConfigurationService(Node handleNode) throws ServiceException {
        super(handleNode);
        this.createdTime = System.currentTimeMillis();
        id = getValueProvider().getName();
        name = getValueProvider().getName();
        sitemapItemHandlerClassName = getValueProvider().getString(HstNodeTypes.SITEMAPITEMHANDLDER_PROPERTY_CLASSNAME);
        if(sitemapItemHandlerClassName == null || "".equals(sitemapItemHandlerClassName)) {
            this.closeValueProvider(false);
            throw new ServiceException("Invalid sitemap item handler because property '"+HstNodeTypes.SITEMAPITEMHANDLDER_PROPERTY_CLASSNAME+"' is missing or empty ");
        }
        
        properties = getValueProvider().getProperties();
        
    }
    public String getSiteMapItemHandlerClassName() {
        return sitemapItemHandlerClassName;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    /**
     * Returns a property of type String, Boolean, Long, Double or Calendar or an array of one of these objects or <code>null</code> when not present
     */
    public Object getProperty(String name) {
        return properties.get(name);
    }

    /**
     * Returns the map of property names to their value. The value can be of type String, Boolean, Long, Double or Calendar or an array of one of these objects
     */
    public Map<String, Object> getProperties() {
        return properties;
    }

    public Service[] getChildServices() {
        return null;
    }
    public long getCreatedTime() {
        return this.createdTime;
    }

}
