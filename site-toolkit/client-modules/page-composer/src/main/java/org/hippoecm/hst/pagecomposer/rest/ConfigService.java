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
package org.hippoecm.hst.pagecomposer.rest;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.xml.ws.Response;

import org.hippoecm.hst.services.support.jaxrs.content.BaseHstContentService;

/**
 * HstConfigService provides the JAX-RS service to GET and POST HST components
 */
@Path("/configservice/")
public class ConfigService extends BaseHstContentService {

    public ConfigService() {
        super();
    }

    /**
     * Responds to /component_path URI and sends JSON representation of the specified component at path.
     * The path is relative to hst:configuration/hst:configuration
     *
     * @param servletRequest  Injected by the context
     * @param servletResponse Injected by the context
     * @param path            The relative path of the node  (from hst:configuration/hst:configuration) for which the JSON is needed.
     * @return JSON Representation of the node specified by path.
     */
    @GET
    @Path("/{path:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    public ComponentWrapper getNode(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @PathParam("path") String path) {
        try {
            Node jcrNode = getJcrSession(servletRequest).getRootNode().getNode(path);
            return new ComponentWrapper(jcrNode);
        } catch (RepositoryException e) {
            e.printStackTrace(); //TODO fix me
            throw new WebApplicationException(e);
        } catch(Exception e) {
            e.printStackTrace(); //TODO fix me
            throw new WebApplicationException(e);
        }
    }

    @POST
    @Path("/{path:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response setNode(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @PathParam("path") String path, String json) {
        Response r = null;
            
        return r;
    }
}
