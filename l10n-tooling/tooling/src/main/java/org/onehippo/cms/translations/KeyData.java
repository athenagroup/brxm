/*
 *  Copyright 2016-2016 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms.translations;

import java.util.HashMap;
import java.util.Map;

import static org.onehippo.cms.translations.KeyData.KeyStatus.CLEAN;
import static org.onehippo.cms.translations.KeyData.LocaleStatus.RESOLVED;


public class KeyData {

    public enum KeyStatus {
        ADDED,
        UPDATED,
        DELETED,
        CLEAN
    }
    
    public enum LocaleStatus {
        RESOLVED,
        UNRESOLVED
    }

    private KeyStatus status;
    private Map<String, LocaleStatus> locales;
    
    public KeyData() {
        status = CLEAN;
    }
    
    public KeyData(KeyStatus status) {
        this.status = status;
    }

    public KeyStatus getStatus() {
        return status;
    }

    public void setStatus(final KeyStatus status) {
        this.status = status;
    }
    
    public Map<String, LocaleStatus> getLocales() {
        return locales;
    }
    
    public void setLocales(Map<String, LocaleStatus> locales) {
        this.locales = locales;
    }

    public LocaleStatus getLocaleStatus(final String locale) {
        if (locales != null && locales.containsKey(locale)) {
            return locales.get(locale);
        }
        return RESOLVED;
    }

    public void setLocaleStatus(final String locale, LocaleStatus localeStatus) {
        if (localeStatus == RESOLVED && locales.size() == 1 && locales.containsKey(locale)) {
            locales = null;
            status = CLEAN;
        } else {
            if (locales == null) {
                locales = new HashMap<>();
            }
            locales.put(locale, localeStatus);
        }
    } 
}
