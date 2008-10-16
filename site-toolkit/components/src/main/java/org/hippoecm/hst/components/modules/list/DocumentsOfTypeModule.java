package org.hippoecm.hst.components.modules.list;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;

import org.hippoecm.hst.core.HSTHttpAttributes;
import org.hippoecm.hst.core.mapping.URLMapping;
import org.hippoecm.hst.core.template.ContextBase;
import org.hippoecm.hst.core.template.HstFilterBase;
import org.hippoecm.hst.core.template.TemplateException;
import org.hippoecm.hst.core.template.module.ModuleBase;
import org.hippoecm.hst.core.template.module.query.ContextWhereClause;
import org.hippoecm.hst.core.template.node.el.ContentELNodeImpl;
import org.hippoecm.hst.core.template.node.el.ELNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentsOfTypeModule extends ModuleBase {
	
	private static final Logger log = LoggerFactory.getLogger(DocumentsOfTypeModule.class);
	public static final String DOCUMENT_TYPE = "documentType";
	private String docType="";
	
	public void render(PageContext pageContext) throws TemplateException{
        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
        ContextBase ctxBase = (ContextBase) request.getAttribute(HstFilterBase.CONTENT_CONTEXT_REQUEST_ATTRIBUTE);
        URLMapping urlMapping = (URLMapping)request.getAttribute(HSTHttpAttributes.URL_MAPPING_ATTR);
        List<ELNode> wrappedNodes = new ArrayList<ELNode>();
        
        boolean params = false;
        if (moduleParameters != null) {
            params = true;
        }
        if (params && moduleParameters.containsKey(DOCUMENT_TYPE)) {
            String type = moduleParameters.get(DOCUMENT_TYPE);
            if (!"".equals(type)) {
                setDocumentType(type);
            }
        }
        
        Session jcrSession = ctxBase.getSession();
        wrappedNodes = getDocuments(jcrSession,urlMapping,ctxBase.getContextRootNode());
        pageContext.setAttribute(getVar(), wrappedNodes);
	
	}
        
    private void setDocumentType(String type){
      this.docType=type;	
    }
	
	private List<ELNode> getDocuments(Session jcrSession, URLMapping urlMapping, Node contextNode){
		List<ELNode> wrappedNodes = new ArrayList<ELNode>();
		if(docType!=null || !docType.equals("")) {
	        try {
	            ContextWhereClause ctxWhereClause = new ContextWhereClause(contextNode, "content");
	            String contextWhereClauses = ctxWhereClause.getWhereClause();
	            String xpath = "//*["+contextWhereClauses+ " and @jcr:primaryType='"+docType+"']";
	            
	            QueryManager qMgr = jcrSession.getWorkspace().getQueryManager();
	            QueryResult result = qMgr.createQuery(xpath,Query.XPATH).execute();
	            NodeIterator iter = result.getNodes();
	            while (iter.hasNext()) {
	                Node node = iter.nextNode();
	                if (node != null && !node.getName().equals("hippo:prototype")) {
	                	wrappedNodes.add(new ContentELNodeImpl(node,urlMapping));
	                }
	            }
	            
	        } catch (RepositoryException ex) {
	           ex.printStackTrace();
	        }
		}
        return wrappedNodes;
	}
}
