/*
 *  Copyright 2009-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.demo.components;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.hippoecm.hst.component.support.bean.BaseHstComponent;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FacetedAddProduct extends BaseHstComponent {

    public static final Logger log = LoggerFactory.getLogger(FacetedAddProduct.class);
  
    @Override
    public void doAction(HstRequest request, HstResponse response) throws HstComponentException {
       
        DummyCarDocsCreator carCreator = new DummyCarDocsCreator();

        // we need an admin session here to be able to do plain jcr writes
        try {
            // since admin session fetched via pooled session impersonation log out will be handled
            Session admin = request.getRequestContext().getSession().impersonate(new SimpleCredentials("admin", new char[0]));
            String rootByPath = request.getRequestContext().getResolvedMount().getMount().getContentPath();
            rootByPath = PathUtils.normalizePath(rootByPath);
            String numberStr = request.getParameter("number");
            int number = Integer.parseInt(numberStr);
            
            carCreator.createCars(admin, rootByPath, number);
            
        } catch (RepositoryException e) {
            log.error(e.getMessage());
        } catch (NumberFormatException e) {
            log.error(e.getMessage());
        }
              
    }
       
}