package org.hippoecm.hst.core.template.node;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;

import org.hippoecm.hst.core.template.ContextBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModuleNode extends TemplateNode {
	private Logger log = LoggerFactory.getLogger(ModuleNode.class);	 
	
	public static final String TEMPLATE_PROPERTY_NAME = "hst:template";
	public static final String CONTENTLOCATION_PROPERTY_NAME = "hst:contentlocation";
	
	public PageContainerModuleNode pageContainerModuleNode;
	
	   public ModuleNode(ContextBase contextBase, Node jcrModuleNode) {
		   super(contextBase, jcrModuleNode);	   
	   }
	   
	   public ModuleNode(ContextBase contextBase, String relativePath) throws RepositoryException{
		   super(contextBase, relativePath);	   
	   }
	   
	   public String getTemplatePage() throws RepositoryException {
		   return jcrNode.getProperty(TEMPLATE_PROPERTY_NAME).getValue().getString();
	   }
	   
	   public Node getContentLocation() throws RepositoryException {	   
		    return getTemplateNodeFromPropertyValue(CONTENTLOCATION_PROPERTY_NAME);
	   }
	  
	   public void setPageContainerModuleNode(PageContainerModuleNode pageContainerModuleNode) {
		   this.pageContainerModuleNode = pageContainerModuleNode;
	   }
	   
	   public String getPropertyValue(String propertyName) throws PathNotFoundException, ValueFormatException, RepositoryException {
		   try {
			if (pageContainerModuleNode != null) {
				  Property property = pageContainerModuleNode.getJcrNode().getProperty(propertyName);
				  if (property != null) {
					 return property.getString();
				  }
			   }
			} catch (Exception e) {			
				log.error("Cannot get property " + propertyName + " from " + pageContainerModuleNode.getJcrNode().getPath());
				//e.printStackTrace();
			}
		
			//try to get property from the moduleNode
		    return super.getPropertyValue(propertyName);
	   }
	   
	   
}
