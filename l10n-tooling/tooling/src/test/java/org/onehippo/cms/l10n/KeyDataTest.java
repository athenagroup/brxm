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
package org.onehippo.cms.l10n;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class KeyDataTest {

    @Test
    public void test_locales_are_nullified_and_status_is_cleared_when_last_locale_is_resolved() {
        KeyData keyData = new KeyData(KeyData.KeyStatus.ADDED);
        keyData.setLocaleStatus("de", KeyData.LocaleStatus.UNRESOLVED);
        keyData.setLocaleStatus("fr", KeyData.LocaleStatus.UNRESOLVED);

        keyData.setLocaleStatus("de", KeyData.LocaleStatus.RESOLVED);
        assertNotNull(keyData.getLocales());
        Assert.assertEquals(KeyData.KeyStatus.ADDED, keyData.getStatus());

        keyData.setLocaleStatus("fr", KeyData.LocaleStatus.RESOLVED);
        assertNull(keyData.getLocales());
        Assert.assertEquals(KeyData.KeyStatus.CLEAN, keyData.getStatus());
    }
}