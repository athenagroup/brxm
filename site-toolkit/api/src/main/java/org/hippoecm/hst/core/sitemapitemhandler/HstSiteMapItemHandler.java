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
package org.hippoecm.hst.core.sitemapitemhandler;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.core.request.SiteMapItemHandlerConfiguration;

/**
 * TODO A HstSiteMapItemHandler can be invoked by .... 
 * 
 */
public interface HstSiteMapItemHandler {
    
    /**
     * Allows the HstSiteMapItemHandler to initialize itself
     * 
     * @param servletConfig the servletConfig of the HST container servlet
     * @param handlerConfig the componentConfigBean configuration
     * @throws HstSiteMapItemHandlerException
     */
    void init(ServletContext servletContext, SiteMapItemHandlerConfiguration handlerConfig) throws HstSiteMapItemHandlerException;
    
    /**
     * 
     * @param resolvedSiteMapItem
     * @param request
     * @param response
     * @return
     * @throws HstSiteMapItemHandlerException
     */
    ResolvedSiteMapItem process(ResolvedSiteMapItem resolvedSiteMapItem, HttpServletRequest request, HttpServletResponse response) throws HstSiteMapItemHandlerException;
    
    /**
     * Through the {@link SiteMapItemHandlerConfiguration} all (resolved) configuration properties can be accessed. 
     * @return the SiteMapItemHandlerConfiguration backing this HstSiteMapItemHandler
     */
    SiteMapItemHandlerConfiguration getSiteMapItemHandlerConfiguration();
    
    /**
     * @return Retrieves the servletContext to which this HstSiteMapItemHandler is bound
     */
    ServletContext getServletContext();
    
    /**
     * Allows the sitemap handler to destroy itself
     * 
     * @throws HstSiteMapItemHandlerException
     */
    void destroy() throws HstSiteMapItemHandlerException;
    
}
