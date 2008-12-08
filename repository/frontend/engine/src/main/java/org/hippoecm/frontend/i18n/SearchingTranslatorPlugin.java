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
package org.hippoecm.frontend.i18n;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import org.apache.wicket.Session;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.plugin.IPlugin;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.ISO9075Helper;
import org.hippoecm.repository.api.NodeNameCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SearchingTranslatorPlugin extends AbstractTranslateService implements IPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    final static Logger log = LoggerFactory.getLogger(SearchingTranslatorPlugin.class);

    public SearchingTranslatorPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
    }

    public IModel getModel(Map<String, String> criteria) {
        try {
            QueryManager qMgr = ((UserSession) Session.get()).getQueryManager();
            String strQuery = "//element(*, hippo:translated)[fn:name()='"
                    + ISO9075Helper.encodeLocalName(NodeNameCodec.encode(criteria.get("hippo:key")))
                    + "']/element(hippo:translation, hippo:translation)[@hippo:language='"
                    + NodeNameCodec.encode(criteria.get("hippo:language")) + "']";
            Query query = qMgr.createQuery(strQuery, Query.XPATH);
            NodeIterator nodes = query.execute().getNodes();
            if (nodes.getSize() > 0) {
                Set<NodeWrapper> list = new HashSet<NodeWrapper>((int) nodes.getSize());
                while (nodes.hasNext()) {
                    list.add(new NodeWrapper(nodes.nextNode(), criteria));
                }
                return new TranslationSelectionStrategy<IModel>(criteria.keySet()).select(list).getModel();
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return null;
    }

}
