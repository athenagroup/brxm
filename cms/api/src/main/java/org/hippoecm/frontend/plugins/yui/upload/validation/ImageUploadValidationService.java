/*
 * Copyright 2012 Hippo.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.frontend.plugins.yui.upload.validation;

import java.awt.Dimension;
import java.io.IOException;

import org.apache.sanselan.ImageReadException;
import org.apache.sanselan.Sanselan;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.frontend.validation.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImageUploadValidationService extends DefaultUploadValidationService {

    private static final Logger log = LoggerFactory.getLogger(ImageUploadValidationService.class);

    public static final int DEFAULT_MAX_WIDTH = 1280;
    public static final int DEFAULT_MAX_HEIGHT = 1024;
    public static final String DEFAULT_MAX_SIZE = "500kb";
    public static final String[] DEFAULT_EXTENSIONS_ALLOWED = new String[]{"*.jpg", "*.jpeg", "*.gif", "*.png"};

    int maxWidth;
    int maxHeight;

    public ImageUploadValidationService(IValueMap params) {
        super(params);

        maxWidth = params.getInt("max.width", getDefaultMaxWidth());
        maxHeight = params.getInt("max.height", getDefaultMaxHeight());
    }

    @Override
    public void validate(final FileUpload upload) throws ValidationException {
        super.validate(upload);
        validateSizes(upload);
    }

    private void validateSizes(final FileUpload upload) {
        String fileName = upload.getClientFileName();

        Dimension dim;
        try {
            dim = Sanselan.getImageSize(upload.getInputStream(), fileName);
        } catch (IOException e) {
            addViolation("upload.validation.io.exception", fileName);
            log.error("Error processing upload", e);
            return;
        } catch (ImageReadException e) {
            addViolation("upload.validation.imageread.exception", fileName);
            log.error("Error processing upload", e);
            return;
        }

        //check image dimensions
        boolean tooWide = maxWidth > 0 && dim.width > maxWidth;
        boolean tooHigh = maxHeight > 0 && dim.height > maxHeight;

        if (tooWide && tooHigh) {
            addViolation("upload.validation.image.width-height", fileName, dim.width, dim.height, maxWidth, maxHeight);
            if (log.isDebugEnabled()) {
                log.debug("Image '{}' resolution is too high ({},{}). The max allowed width is ({}, {})",
                        new Object[]{fileName, dim.width, dim.height, maxWidth, maxHeight});

            }
        } else if (tooWide) {
            addViolation("upload.validation.image.width", fileName, dim.width, maxWidth);
            if (log.isDebugEnabled()) {
                log.debug("Image '{}' is {} pixels wide while the max allowed width is {}",
                        new Object[]{fileName, dim.width, maxWidth});
            }
        } else if (tooHigh) {
            addViolation("upload.validation.image.height", fileName, dim.height, maxHeight);
            if (log.isDebugEnabled()) {
                log.debug("Image '{}' is {} pixels high while the max allowed height is {}",
                        new Object[]{fileName, dim.height, maxHeight});
            }
        }
    }

    protected int getDefaultMaxWidth() {
        return DEFAULT_MAX_WIDTH;
    }

    protected int getDefaultMaxHeight() {
        return DEFAULT_MAX_HEIGHT;
    }

    @Override
    protected String[] getDefaultExtensionsAllowed() {
        return DEFAULT_EXTENSIONS_ALLOWED;
    }

    @Override
    protected String getDefaultMaxFileSize() {
        return DEFAULT_MAX_SIZE;
    }

}
