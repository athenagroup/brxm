/*
 * Copyright 2014-2017 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.rest.model;

import java.util.Calendar;

import org.junit.Test;
import org.onehippo.cms7.essentials.WebUtils;
import org.onehippo.cms7.essentials.dashboard.model.PluginDescriptorRestful;
import org.onehippo.cms7.essentials.dashboard.model.Repository;
import org.onehippo.cms7.essentials.dashboard.model.RepositoryPolicy;
import org.onehippo.cms7.essentials.dashboard.model.RepositoryPolicyRestful;
import org.onehippo.cms7.essentials.dashboard.model.RepositoryRestful;
import org.onehippo.cms7.essentials.dashboard.model.TargetPom;
import org.onehippo.cms7.essentials.dashboard.model.Vendor;
import org.onehippo.cms7.essentials.dashboard.model.VendorRestful;
import org.onehippo.cms7.essentials.dashboard.rest.PluginModuleRestful;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;

public class PluginDescriptorRestfulTest {

    private static Logger log = LoggerFactory.getLogger(PluginDescriptorRestfulTest.class);

    @Test
    public void testJaxb() throws Exception {
        PluginDescriptorRestful value = new PluginDescriptorRestful();
        value.setName("com.foo.name");
        final Calendar today = Calendar.getInstance();
        value.setDateInstalled(today);
        value.addRestCLass("com.foo.Foo");
        value.addRestCLass("com.foo.Bar");
        // add libraries:
        final PluginModuleRestful.PrefixedLibrary library = new PluginModuleRestful.PrefixedLibrary();
        value.addLibrary(library);
        library.addLibrary(new PluginModuleRestful.Library("myPlugin", "foo.js"));
        library.addLibrary(new PluginModuleRestful.Library("myPlugin1", "foo1.js"));

        final Vendor vendor = new VendorRestful();
        vendor.setName("hippo");
        value.setVendor(vendor);

        final Repository repository = new RepositoryRestful();
        repository.setId("myId");
        repository.setUrl("http://onehippo.com/maven2");
        final RepositoryPolicy snapshots = new RepositoryPolicyRestful();
        snapshots.setChecksumPolicy("test-snapshots-checksumpolicy");
        snapshots.setUpdatePolicy("test-snapshots-updatepolicy");
        repository.setSnapshots(snapshots);
        final RepositoryPolicy repositoryPolicy = new RepositoryPolicyRestful();
        repositoryPolicy.setChecksumPolicy("testchecksumpolicy");
        repositoryPolicy.setUpdatePolicy("testupdatepolicy");
        repository.setReleases(repositoryPolicy);
        repository.setTargetPom(TargetPom.PROJECT.getName());
        value.addRepository(repository);

        // test json:

        final String json = WebUtils.toJson(value);
        log.info("value {}", value);
        log.info("json {}", json);
        final PluginDescriptorRestful fromJson = value = WebUtils.fromJson(json, PluginDescriptorRestful.class);
        log.info("fromJson {}", fromJson);
        assertEquals(2, fromJson.getRestClasses().size());
        assertEquals(today.getTime(), fromJson.getDateInstalled().getTime());
        assertEquals(vendor.getName(), fromJson.getVendor().getName());
        assertEquals("Expected 1 prefixed library", 1, value.getLibraries().size());
        assertEquals("Expected 2 js libraries", 2, value.getLibraries().get(0).getItems().size());
        assertEquals("Expected 1 repository", 1, value.getRepositories().size());
        assertEquals("Expected repository checksum policy", "testchecksumpolicy", value.getRepositories().get(0).getReleases().getChecksumPolicy());
        assertEquals("Expected repository update policy", "testupdatepolicy", value.getRepositories().get(0).getReleases().getUpdatePolicy());
        assertEquals("Expected repository enabled not to be set", null, value.getRepositories().get(0).getReleases().getEnabled());
        assertEquals("Expected repository snapshots checksum policy", "test-snapshots-checksumpolicy", value.getRepositories().get(0).getSnapshots().getChecksumPolicy());
        assertEquals("Expected repository snapshots update policy", "test-snapshots-updatepolicy", value.getRepositories().get(0).getSnapshots().getUpdatePolicy());
        assertEquals("Expected repository snapshots enabled not to be set", null, value.getRepositories().get(0).getSnapshots().getEnabled());

    }
}
