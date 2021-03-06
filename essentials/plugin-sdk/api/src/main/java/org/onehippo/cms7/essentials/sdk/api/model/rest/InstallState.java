/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.sdk.api.model.rest;

/**
 * InstallState defines the <em>ordered</em> states a plugin goes through during its installation.
 */
public enum InstallState {
    DISCOVERED("discovered"),
    INSTALLATION_PENDING("installationPending"),
    AWAITING_USER_INPUT("awaitingUserInput"),
    INSTALLING("installing"),
    INSTALLED("installed");

    private final String name;

    InstallState(String name) {
        this.name = name;
    }

    public String toString() {
        return name;
    }

    public static InstallState fromString(final String name) {
        for (InstallState s : InstallState.values()) {
            if (s.name.equals(name)) {
                return s;
            }
        }
        return null;
    }
}
