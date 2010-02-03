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
package org.hippoecm.hst.core.container;

import java.util.Collection;

import org.hippoecm.hst.configuration.components.HstComponentInfo;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.util.KeyValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DefaultPageErrorHandler
 * 
 * @version $Id$
 */
public class DefaultPageErrorHandler implements PageErrorHandler {
    
    protected final static Logger log = LoggerFactory.getLogger(DefaultPageErrorHandler.class);
    
    public Object handleComponentExceptions(Collection<KeyValue<HstComponentInfo, Collection<HstComponentException>>> componentExceptionPairs, HstRequest hstRequest, HstResponse hstResponse) {
        logWarningsForEachComponentExceptions(componentExceptionPairs);
        return HANDLED_BUT_CONTINUE;
    }
    
    protected void logWarningsForEachComponentExceptions(Collection<KeyValue<HstComponentInfo, Collection<HstComponentException>>> componentExceptionPairs) {
        for (KeyValue<HstComponentInfo, Collection<HstComponentException>> pair : componentExceptionPairs) {
            HstComponentInfo componentInfo = pair.getKey();
            
            for (HstComponentException componentException : pair.getValue()) {
                if (log.isDebugEnabled()) {
                    log.warn("Component exception found on " + componentInfo.getComponentClassName(), componentException);
                } else if (log.isWarnEnabled()) {
                    log.warn("Component exception found on {}. ", componentInfo.getComponentClassName(), componentException.toString());
                }
            }
        }
    }
    
}
