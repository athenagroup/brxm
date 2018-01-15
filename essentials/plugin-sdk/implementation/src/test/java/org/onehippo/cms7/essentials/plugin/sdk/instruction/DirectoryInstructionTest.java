/*
 * Copyright 2015-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.essentials.plugin.sdk.instruction;


import java.nio.file.Path;

import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Test;
import org.onehippo.cms7.essentials.BaseResourceTest;
import org.onehippo.cms7.essentials.plugin.sdk.ctx.PluginContext;
import org.onehippo.cms7.essentials.plugin.sdk.install.Instruction;
import org.onehippo.cms7.essentials.plugin.sdk.service.ProjectService;
import org.onehippo.cms7.essentials.plugin.sdk.service.model.Module;

import static org.junit.Assert.assertEquals;

public class DirectoryInstructionTest extends BaseResourceTest {

    @Inject private ProjectService projectService;

    @Test
    public void test_create() throws Exception {
        final DirectoryInstruction instruction = new DirectoryInstruction();
        instruction.setAction("create");
        instruction.setTarget(getTargetPath().toString());
        assertEquals(Instruction.Status.SUCCESS, instruction.execute(getContext()));
    }

    @Test
    public void testCopy() throws Exception {
        final PluginContext context = getContext();
        final DirectoryInstruction instruction = new DirectoryInstruction();
        assertEquals(Instruction.Status.FAILED, instruction.execute(context));

        instruction.setSource("/instructions");
        instruction.setAction("copy");
        instruction.setTarget(getTargetPath().toString());
        assertEquals(Instruction.Status.FAILED, instruction.execute(context)); // Fails because unit test is not running in JAR
    }

    @Override
    @After
    public void tearDown() throws Exception {
        FileUtils.deleteDirectory(getTargetPath().toFile());
    }

    private Path getTargetPath() {
        return projectService.getBasePathForModule(Module.PROJECT).resolve("instructions");
    }
}
