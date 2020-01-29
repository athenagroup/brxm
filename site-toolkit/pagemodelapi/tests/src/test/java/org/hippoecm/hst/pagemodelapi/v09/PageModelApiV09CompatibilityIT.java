/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.pagemodelapi.v09;


import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletException;

import org.apache.commons.io.IOUtils;
import org.hippoecm.hst.pagemodelapi.common.AbstractPageModelApiTestCases;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * <p>
 *     This integration test will fail if the v09 Page Model API changes backward incompatible. Backward incompatible
 *     changes are
 *     <ol>
 *         <li>Removing fields</li>
 *         <li>Changing the value of a field, for example a site link value</li>
 *         <li>Changing the order of arrays</li>
 *     </ol>
 *     Backward compatible changes are
 *     <ol>
 *         <li>Adding new fields</li>
 *         <li>Reodering objects</li>
 *     </ol>
 * </p>
 */
public class PageModelApiV09CompatibilityIT extends AbstractPageModelApiTestCases {

    @Test
    public void homepage_api_compatibility_v09_assertion() throws Exception {

        String actual = getActualJson("/spa/resourceapi/home");

        InputStream inputStream = PageModelApiV09CompatibilityIT.class.getResourceAsStream("pma_homepage.json");

        String expected = IOUtils.toString(inputStream, "UTF-8");

        JSONAssert.assertEquals(expected, actual, JSONCompareMode.STRICT_ORDER);
    }

    @Test
    public void newspage_api_compatibility_v09_assertion() throws Exception {

        String actual = getActualJson("/spa/resourceapi/news/News1");

        InputStream inputStream = PageModelApiV09CompatibilityIT.class.getResourceAsStream("pma_newspage.json");

        String expected = IOUtils.toString(inputStream, "UTF-8");

        JSONAssert.assertEquals(expected, actual, JSONCompareMode.STRICT_ORDER);
    }

    @NotNull
    private String getActualJson(final String pathInfo) throws IOException, ServletException {
        final RequestResponseMock requestResponse = mockGetRequestResponse(
                "http", "localhost", pathInfo, null);

        final MockHttpServletResponse response = render(requestResponse);

        return response.getContentAsString();
    }

}
