/*
 * Copyright 2014-2017 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.dashboard.blog;

import java.util.function.BiConsumer;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.instructions.Instruction;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionStatus;
import org.onehippo.cms7.essentials.dashboard.packaging.MessageGroup;
import org.onehippo.cms7.essentials.dashboard.utils.EssentialConst;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
public class BlogFacetInstruction implements Instruction {

    private static final String DATE_FACET = ":publicationdate$[\n" +
            "                {name:'Last 7 days',resolution:'day', begin:-6, end:1},\n" +
            "                {name:'Last month',resolution:'day', begin:-30, end:1},\n" +
            "                {name:'Last 3 months',resolution:'day', begin:-91, end:1},\n" +
            "                {name:'Last 6 months',resolution:'day', begin:-182, end:1},\n" +
            "                {name:'Last year',resolution:'day', begin:-365, end:1}\n" +
            "                ]";
    public static final long DEFAULT_FACET_LIMIT = 100L;
    private static Logger log = LoggerFactory.getLogger(BlogFacetInstruction.class);

    @Override
    public InstructionStatus execute(final PluginContext context) {
        final String namespace = (String) context.getPlaceholderData().get(EssentialConst.PLACEHOLDER_NAMESPACE);
        final String targetNode = "/content/documents/" + namespace;
        Session session = null;
        try {
            session = context.createSession();
            final Node root = session.getNode(targetNode);
            final String facetName = "blogFacets";
            if (root.hasNode(facetName)) {
                root.getNode(facetName).remove();
            }

            final Node blogFacets = root.addNode(facetName, "hippofacnav:facetnavigation");
            final String docRef = session.getNode(targetNode + "/blog").getIdentifier();
            blogFacets.setProperty("hippo:docbase", docRef);
            blogFacets.setProperty("hippo:count", "0");
            blogFacets.setProperty("hippofacnav:facetnodenames", new String[]{"Authors", "Categories", "Tags", "Date"});
            blogFacets.setProperty("hippofacnav:facets", new String[]{namespace + ":authornames", namespace + ":categories", "hippostd:tags", namespace + DATE_FACET});
            blogFacets.setProperty("hippofacnav:filters", new String[]{"jcr:primaryType = " + namespace + ":blogpost"});
            blogFacets.setProperty("hippofacnav:sortby", new String[]{namespace + ":publicationdate"});
            blogFacets.setProperty("hippofacnav:sortorder", new String[]{"descending"});
            blogFacets.setProperty("hippofacnav:limit", DEFAULT_FACET_LIMIT);
            session.save();

        } catch (RepositoryException e) {
            log.error("Error creating blog facet", e);
            return InstructionStatus.FAILED;
        } finally {
            GlobalUtils.cleanupSession(session);
        }


        return InstructionStatus.SUCCESS;
    }

    @Override
    public void populateChangeMessages(final BiConsumer<MessageGroup, String> changeMessageQueue) {
        changeMessageQueue.accept(MessageGroup.EXECUTE, "Create blog facet at: /content/documents/{{namespace}}/blogFacets");
    }
}
