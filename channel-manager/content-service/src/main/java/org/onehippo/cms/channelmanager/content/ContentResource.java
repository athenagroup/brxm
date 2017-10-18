/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms.channelmanager.content;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.onehippo.cms.channelmanager.content.document.DocumentsService;
import org.onehippo.cms.channelmanager.content.document.model.Document;
import org.onehippo.cms.channelmanager.content.document.model.FieldValue;
import org.onehippo.cms.channelmanager.content.document.util.FieldPath;
import org.onehippo.cms.channelmanager.content.documenttype.DocumentTypesService;
import org.onehippo.cms.channelmanager.content.error.ErrorWithPayloadException;
import org.onehippo.cms7.services.cmscontext.CmsSessionContext;
import org.onehippo.repository.jaxrs.api.SessionDataProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Produces("application/json")
@Path("/")
public class ContentResource {

    private static final Logger log = LoggerFactory.getLogger(ContentResource.class);

    private static final CacheControl NO_CACHE = new CacheControl();

    static {
        NO_CACHE.setNoCache(true);
    }

    private final SessionDataProvider sessionDataProvider;
    private final DocumentsService documentService;
    private final ContextPayloadProvider contextPayloadProvider;

    public ContentResource(final SessionDataProvider userSessionProvider, final DocumentsService documentsService) {
        this(userSessionProvider, documentsService, new ContextPayloadProvider());
    }

    ContentResource(final SessionDataProvider userSessionProvider, final DocumentsService documentsService,
                    final ContextPayloadProvider contextPayloadProvider) {
        this.sessionDataProvider = userSessionProvider;
        this.documentService = documentsService;
        this.contextPayloadProvider = contextPayloadProvider;
    }

    @POST
    @Path("documents/{id}/draft")
    public Response createDraftDocument(@PathParam("id") String id, @Context HttpServletRequest servletRequest) {
        return executeTask(servletRequest, Status.CREATED,
                (session, locale, contextPayload) -> documentService.createDraft(id, session, locale, contextPayload));
    }

    @PUT
    @Path("documents/{id}/draft")
    public Response updateDraftDocument(@PathParam("id") String id, Document document,
                                        @Context HttpServletRequest servletRequest) {
        return executeTask(servletRequest, Status.OK,
                (session, locale, contextPayload) -> documentService.updateDraft(id, document, session, locale, contextPayload));
    }

    @PUT
    @Path("documents/{documentId}/draft/{fieldPath:.*}")
    public Response updateDraftField(@PathParam("documentId") String documentId,
                                     @PathParam("fieldPath") String fieldPath,
                                     List<FieldValue> fieldValues,
                                     @Context HttpServletRequest servletRequest) {
        return executeTask(servletRequest, Status.NO_CONTENT, (session, locale, contextPayload) -> {
            documentService.updateDraftField(documentId, new FieldPath(fieldPath), fieldValues, session, locale, contextPayload);
            return null;
        });
    }

    @DELETE
    @Path("documents/{id}/draft")
    public Response deleteDraftDocument(@PathParam("id") String id, @Context HttpServletRequest servletRequest) {
        return executeTask(servletRequest, Status.NO_CONTENT, (session, locale, contextPayload) -> {
            documentService.deleteDraft(id, session, locale, contextPayload);
            return null;
        });
    }

    // for easy debugging:

    @GET
    @Path("documents/{id}")
    public Response getPublishedDocument(@PathParam("id") String id, @Context HttpServletRequest servletRequest) {
        return executeTask(servletRequest, Status.OK,
                (session, locale, contextPayload) -> documentService.getPublished(id, session, locale, contextPayload));
    }

    @GET
    @Path("documenttypes/{id}")
    public Response getDocumentType(@PathParam("id") String id, @Context HttpServletRequest servletRequest) {
        return executeTask(servletRequest, Status.OK, NO_CACHE,
                (session, locale, contextPayload) -> DocumentTypesService.get().getDocumentType(id, session, locale));
    }

    private Response executeTask(final HttpServletRequest servletRequest,
                                 final Status successStatus,
                                 final EndPointTask task) {
        return executeTask(servletRequest, successStatus, null, task);
    }

    /**
     * Shared logic for providing the EndPointTask with contextual input and handling the packaging of its response
     * (which may be an error, encapsulated in an Exception).
     *
     * @param servletRequest current HTTP servlet request to derive contextual input
     * @param successStatus  HTTP status code in case of success
     * @param cacheControl   HTTP Cache-Control response header
     * @param task           the EndPointTask to execute
     * @return a JAX-RS response towards the client
     */
    private Response executeTask(final HttpServletRequest servletRequest,
                                 final Status successStatus,
                                 final CacheControl cacheControl,
                                 final EndPointTask task) {
        final Session session = sessionDataProvider.getJcrSession(servletRequest);
        final Locale locale = sessionDataProvider.getLocale(servletRequest);
        try {
            final Object result = task.execute(session, locale, contextPayloadProvider.getContextPayload(servletRequest));
            return Response.status(successStatus).cacheControl(cacheControl).entity(result).build();
        } catch (ErrorWithPayloadException e) {
            return Response.status(e.getStatus()).cacheControl(cacheControl).entity(e.getPayload()).build();
        }
    }

    @FunctionalInterface
    private interface EndPointTask {
        Object execute(Session session, Locale locale, Map<String, Serializable> contextPayload) throws ErrorWithPayloadException;
    }


    protected static class ContextPayloadProvider {
        protected Map<String, Serializable> getContextPayload(final HttpServletRequest servletRequest) {
            final HttpSession session = servletRequest.getSession();
            final CmsSessionContext context;
            if (session == null || (context = CmsSessionContext.getContext(session)) == null) {
                log.error("HttpSession should not be null and CmsSessionContext should be present");
            } else {
                final Map<String, Serializable> payload = context.getContextPayload();
                if (payload != null) {
                    return payload;
                } else {
                    log.error("No attribute with key '{}' found on CmsSessionContext.", CmsSessionContext.CMS_SESSION_CONTEXT_PAYLOAD_KEY);
                }
            }
            return Collections.emptyMap();
        }
    }
}
