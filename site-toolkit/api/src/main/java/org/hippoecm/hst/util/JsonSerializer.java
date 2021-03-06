/*
 *  Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.util;

public interface JsonSerializer {

    /**
     * @param value the object to write as json
     * @return the json representation of {@code value}
     * @throws JsonSerializationException in case the {@code value} object cannot be serialized to json
     */
    String toJson(Object value);

    class JsonSerializationException extends RuntimeException {
        public JsonSerializationException() {
            super();
        }

        public JsonSerializationException(final String message) {
            super(message);
        }

        public JsonSerializationException(final String message, final Throwable cause) {
            super(message, cause);
        }

        public JsonSerializationException(final Throwable cause) {
            super(cause);
        }

        protected JsonSerializationException(final String message, final Throwable cause, final boolean enableSuppression, final boolean writableStackTrace) {
            super(message, cause, enableSuppression, writableStackTrace);
        }
    }
}
