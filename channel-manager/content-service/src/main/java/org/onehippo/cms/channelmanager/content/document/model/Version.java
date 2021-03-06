/*
 * Copyright 2020 Bloomreach
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
 *
 */
package org.onehippo.cms.channelmanager.content.document.model;

import java.util.Calendar;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Version {

    @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC", pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private final Calendar timestamp;
    private final String userName;
    private final String jcrUUID;
    private final String branchId;

    @JsonCreator
    public Version(
            @JsonProperty("timestamp") Calendar timestamp,
            @JsonProperty("userName") String userName,
            @JsonProperty("jcrUUID") String jcrUUID,
            @JsonProperty("branchId") String branchId
    ) {
        this.timestamp = timestamp;
        this.userName = userName;
        this.jcrUUID = jcrUUID;
        this.branchId = branchId;
    }

    public Calendar getTimestamp() {
        return timestamp;
    }

    public String getUserName() {
        return userName;
    }

    public String getJcrUUID() {
        return jcrUUID;
    }

    public String getBranchId() {
        return branchId;
    }
}
