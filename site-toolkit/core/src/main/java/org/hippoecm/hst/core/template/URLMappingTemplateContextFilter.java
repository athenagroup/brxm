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
package org.hippoecm.hst.core.template;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.hippoecm.hst.core.HSTHttpAttributes;
import org.hippoecm.hst.core.template.node.LayoutNode;
import org.hippoecm.hst.core.template.node.NavigationNode;
import org.hippoecm.hst.core.template.node.PageNode;
import org.hippoecm.hst.core.template.node.TemplateNode;
import org.hippoecm.hst.jcr.JCRConnector;
import org.hippoecm.hst.jcr.JCRConnectorWrapper;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class URLMappingTemplateContextFilter extends HstFilterBase implements Filter {

	public static final String SITEMAP_CONTEXTBASE_NAME = "siteMapContextBase";
	
	
	public static final String JCRSESSION_REQUEST_ATTRIBUTE = "jcrSession";
	//public static final String NAVIGATION_REQUEST_ATTRIBUTE = "hstNavigationMapLocation";
	
	//public static final String NAVIGATION_CONTEXTBASE_REQUEST_ATTRIBUTE = "navigationContextBase";
	//public static final String NAVIGATION_CONTEXTBASE_NAME = "navigationContext";	
	
	public static final String REPOSITORY_LOCATION_FILTER_INIT_PARAM = "hstConfigurationUrl";
	
	private static final Logger log = LoggerFactory.getLogger(URLMappingTemplateContextFilter.class);
	
	private String repositoryTemplateContextLocation = null;
	
	public void init(FilterConfig filterConfig) throws ServletException {
		super.init(filterConfig);
		repositoryTemplateContextLocation = getInitParameter(filterConfig, REPOSITORY_LOCATION_FILTER_INIT_PARAM, true);
	}
	  
	public void destroy() {
	}


	/**
	 * The filter determines the templates to use and the content location from the request URI.
	 * If a template is found the request is forwarded to that template. If not, the filterchain is
	 * continued with the original request.
	 */
	public void doFilter(ServletRequest req, ServletResponse response,
			FilterChain filterChain) throws IOException, ServletException {
		
		//Map<String, PageNode> urlMappingPageNodes;
		
		HttpServletRequest request = (HttpServletRequest) req;
		
		if (ignoreRequest(request)) {
			log.debug("ingore " + request.getRequestURI());
			filterChain.doFilter(request, response);
		} else {
			log.debug("process " + request.getRequestURI());
			
			
			try {
				ContextBase templateContextBase = getHstConfigurationContextBase(request, repositoryTemplateContextLocation);
				//find 
				URLMappingTokenizer urlTokenizer = new URLMappingTokenizer(request, getURLMappingNodes(templateContextBase) );
	           	PageNode matchPageNode = getPageNode(request, urlTokenizer, templateContextBase);
	           	if (matchPageNode != null) {
	            	
	            	log.info("matchPageNode.getURLMappingValue()=" + matchPageNode.getURLMappingValue());
	            	
	            	String urlPrefix = getUrlPrefix(request);
	            	RequestDispatcher dispatcher = request.getRequestDispatcher(urlPrefix + matchPageNode.getLayoutNode().getTemplatePage());
	            	
	            	HttpServletRequestWrapper wrappedRequest = urlTokenizer.getWrappedRequest(request);
	            
	            	//set attributes
	            	wrappedRequest.setAttribute(PAGENODE_REQUEST_ATTRIBUTE, matchPageNode);
	            	//wrappedRequest.setAttribute(JCRSESSION_REQUEST_ATTRIBUTE, JCRConnectorWrapper.getTemplateJCRSession(request.getSession()));
	            	//wrappedRequest.setAttribute(NAVIGATION_CONTEXTBASE_REQUEST_ATTRIBUTE, navigationContextBase);
	            	
	    			dispatcher.forward(wrappedRequest, response);
	            } else {
	            	log.info("no matching template found for" + request.getRequestURI() );
	            	//what to do? no matching pattern found... lets continue the filter chain...
	            	filterChain.doFilter(req, response);
	            }
			} catch (Exception e) {		
				e.printStackTrace();
				filterChain.doFilter(req, response);
			}
		}
	}

	
	
	/**
	 * Determines if the parameter is a 'real' pattern or just a regular String.
	 * (only checks for + and * for now...)
	 * @param s
	 * @return
	 */
	private boolean isPattern(String s) {
		return s.contains("+") ||
		       s.contains("*");
	}
	
	private static Node getNodeByAbsolutePath(final Session session, final String path) throws PathNotFoundException, RepositoryException{
		String relPath = path.startsWith("/") ? path.substring(1) : path;
		return session.getRootNode().getNode(relPath);
	}

}
