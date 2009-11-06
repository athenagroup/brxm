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
package org.hippoecm.hst.services.support.jaxrs.content;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import javax.jcr.RepositoryException;
import javax.xml.bind.annotation.XmlRootElement;

import org.hippoecm.hst.content.beans.standard.HippoDocumentBean;
import org.hippoecm.hst.content.beans.standard.HippoFolderBean;

@XmlRootElement(name = "folder")
public class HippoFolderBeanContent extends HippoBeanContent {
    
    private HippoFolderBeanContent [] folderBeanContents;
    private HippoDocumentBeanContent [] documentBeanContents;
    
    public HippoFolderBeanContent() {
        super();
    }
    
    public HippoFolderBeanContent(String name) {
        super(name);
    }
    
    public HippoFolderBeanContent(String name, String path) {
        super(name, path);
    }
    
    public HippoFolderBeanContent(HippoFolderBean bean) throws RepositoryException {
        super(bean);
        
        ArrayList<HippoFolderBeanContent> folderBeanContentList = new ArrayList<HippoFolderBeanContent>();
        
        for (HippoFolderBean fldrBean : bean.getFolders()) {
            if (fldrBean != null) {
                folderBeanContentList.add(new HippoFolderBeanContent(fldrBean.getName(), fldrBean.getPath()));
            }
        }
        
        folderBeanContents = new HippoFolderBeanContent[folderBeanContentList.size()];
        folderBeanContents = folderBeanContentList.toArray(folderBeanContents);
        
        ArrayList<HippoDocumentBeanContent> documentBeanContentList = new ArrayList<HippoDocumentBeanContent>();
        
        for (HippoDocumentBean docBean : bean.getDocuments()) {
            if (docBean != null) {
                documentBeanContentList.add(new HippoDocumentBeanContent(docBean.getName(), docBean.getPath()));
            }
        }
        
        documentBeanContents = new HippoDocumentBeanContent[documentBeanContentList.size()];
        documentBeanContents = documentBeanContentList.toArray(documentBeanContents);
    }
    
    public HippoFolderBeanContent [] getFolder() {
        return folderBeanContents;
    }
    
    public void setFolder(HippoFolderBeanContent [] folderBeanContents) {
        this.folderBeanContents = folderBeanContents;
    }
    
    public HippoDocumentBeanContent [] getDocument() {
        return documentBeanContents;
    }
    
    public void setDocument(HippoDocumentBeanContent [] documentBeanContents) {
        this.documentBeanContents = documentBeanContents;
    }
    
    @Override
    public void buildChildUrls(String urlBase, String siteContentPath, String encoding) throws UnsupportedEncodingException {
        PropertyContent [] propertyContents = getProperty();
        
        if (propertyContents != null) {
            for (PropertyContent propertyContent : propertyContents) {
                propertyContent.buildUrl(urlBase, siteContentPath, encoding);
            }
        }
        
        if (folderBeanContents != null) {
            for (HippoFolderBeanContent folderContent : folderBeanContents) {
                folderContent.buildUrl(urlBase, siteContentPath, encoding);
            }
        }
        
        if (documentBeanContents != null) {
            for (HippoDocumentBeanContent documentContent : documentBeanContents) {
                documentContent.buildUrl(urlBase, siteContentPath, encoding);
            }
        }
    }
    
    @Override
    public NodeContent [] getNode() {
        return null;
    }
    
}
