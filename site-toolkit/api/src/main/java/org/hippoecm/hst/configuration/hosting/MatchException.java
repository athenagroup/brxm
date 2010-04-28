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
package org.hippoecm.hst.configuration.hosting;

/**
 * Exception which can be thrown when some matching fails, for example because there are no virtual hosts configured
 */
public class MatchException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    /**
     * Constructs a new ContainerException exception.
     */
    public MatchException() {
        super();
    }

    /**
     * Constructs a new ContainerException exception with the given message.
     *
     * @param   message
     *          the exception message
     */
    public MatchException(String message) {
        super(message);
    }

    /**
     * Constructs a new ContainerException exception with the nested exception.
     *
     * @param   nested
     *          the nested exception
     */
    public MatchException(Throwable nested) {
        super(nested);
    }

    /**
     * Constructs a new ContainerException exception when the container needs to do
     * the following:
     * <ul>
     * <li>throw an exception 
     * <li>include the "nested" exception
     * <li>include a description message
     * </ul>
     *
     * @param   msg
     *          the exception message
     * @param   nested
     *          the nested exception
     */
    public MatchException(String msg, Throwable nested) {
        super(msg, nested);
    }
}
