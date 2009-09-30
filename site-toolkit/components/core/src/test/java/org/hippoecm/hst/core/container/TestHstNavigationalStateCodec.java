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

import static org.junit.Assert.assertEquals;

import java.io.UnsupportedEncodingException;

import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class TestHstNavigationalStateCodec {

    @Test
    public void testNavigationalStateCodec() throws ContainerException, UnsupportedEncodingException {
        HstNavigationalStateCodec codec = new HstNavigationalStateCodecImpl();
        String params = "a=1&b=2&path=/test/a.jsp";
        String encoded = codec.encodeParameters(params, null);
        System.out.println("encoded: " + encoded);
        String decoded = codec.decodeParameters(encoded, null);
        System.out.println("decoded: " + decoded);
        assertEquals(params, decoded);
    }

}
