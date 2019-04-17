/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.services.validation.validator;

import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms.services.validation.validator.NonEmptyHtmlValidator;
import org.onehippo.cms7.services.validation.ValidatorConfig;
import org.onehippo.cms7.services.validation.ValidatorContext;
import org.onehippo.cms7.services.validation.exception.InvalidValidatorException;
import org.onehippo.testutils.log4j.Log4jInterceptor;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

public class NonEmptyHtmlValidatorTest {

    private ValidatorContext context;
    private NonEmptyHtmlValidator validator;

    @Before
    public void setUp() {
        final ValidatorConfig config = createMock(ValidatorConfig.class);
        context = createMock(ValidatorContext.class);
        validator = new NonEmptyHtmlValidator(config);
    }

    @Test(expected = InvalidValidatorException.class)
    public void throwsExceptionIfFieldIsNotOfTypeString() throws Exception {
        expect(context.getType()).andReturn("not-a-string");
        replayAll();

        validator.init(context);
    }

    @Test
    public void initializesIfFieldIsOfTypeString() throws Exception {
        expect(context.getType()).andReturn("String");
        expect(context.getName()).andReturn("CustomHtml");
        replayAll();

        validator.init(context);

        verifyAll();
    }

    @Test
    public void warnsIfValidatorIsUsedWithHtmlField() throws Exception {
        expect(context.getType()).andReturn("String");
        expect(context.getName()).andReturn("Html");
        replayAll();

        try (final Log4jInterceptor listener = Log4jInterceptor.onWarn().trap(NonEmptyHtmlValidator.class).build()) {
            try {
                validator.init(context);
            } finally {
                assertEquals(1L, listener.messages().count());
                verifyAll();
            }
        }
    }

    @Test
    public void textIsValid() {
        assertTrue(validator.isValid(context, "text"));
    }

    @Test
    public void paragraphWithTextIsValid() {
        assertTrue(validator.isValid(context, "<p>text</p>"));
    }
    @Test
    public void imgIsValid() {
        assertTrue(validator.isValid(context, "<img src=\"empty.gif\">"));
    }

    @Test
    public void nullIsInvalid() {
        assertFalse(validator.isValid(context, null));
    }

    @Test
    public void blankStringIsInvalid() {
        assertFalse(validator.isValid(context, ""));
        assertFalse(validator.isValid(context, " "));
    }

    @Test
    public void emptyHtmlIsInvalid() {
        assertFalse(validator.isValid(context, "<html></html>"));
    }

    @Test
    public void emptyParagraphInvalid() {
        assertFalse(validator.isValid(context, "<p></p>"));
    }

}
