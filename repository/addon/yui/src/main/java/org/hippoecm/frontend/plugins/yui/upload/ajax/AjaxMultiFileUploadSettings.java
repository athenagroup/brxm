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
package org.hippoecm.frontend.plugins.yui.upload.ajax;

import org.hippoecm.frontend.plugins.yui.AjaxSettings;

public class AjaxMultiFileUploadSettings extends AjaxSettings {
    final static String SVN_ID = "$Id$";

    //URL of the Flash file
    private String flashUrl;

    //URL that is used to upload files; must include a session ID
    private String uploadUrl;

    //Allowed file extensions; format is [ "*.jpg", "*.gif" ] etc
    private String[] fileExtensions;

    //Set to true if Browse dialog should allow multiple file selection
    private boolean allowMultipleFiles = true;

    //Max number of simultaneous uploads 
    private int simultaneousUploadLimit = 3;

    //Id of the ajaxIndicatorObject
    private String ajaxIndicatorId;

    public boolean isAllowMultipleFiles() {
        return allowMultipleFiles;
    }

    public void setAllowMultipleFiles(boolean allowMultipleFiles) {
        this.allowMultipleFiles = allowMultipleFiles;
    }

    public int getSimultaneousUploadLimit() {
        return simultaneousUploadLimit;
    }

    public void setSimultaneousUploadLimit(int simultaneousUploadLimit) {
        this.simultaneousUploadLimit = simultaneousUploadLimit;
    }

    public String[] getFileExtensions() {
        return fileExtensions;
    }

    public void setFileExtensions(String[] fileExtensions) {
        this.fileExtensions = fileExtensions;
    }

    public String getUploadUrl() {
        return uploadUrl;
    }

    public void setUploadUrl(String uploadUrl) {
        this.uploadUrl = uploadUrl;
    }

    public String getFlashUrl() {
        return flashUrl;
    }

    public void setFlashUrl(String flashUrl) {
        this.flashUrl = flashUrl;
    }

    public String getAjaxIndicatorId() {
        return ajaxIndicatorId;
    }

    public void setAjaxIndicatorId(String ajaxIndicatorId) {
        this.ajaxIndicatorId = ajaxIndicatorId;
    }


    
}
