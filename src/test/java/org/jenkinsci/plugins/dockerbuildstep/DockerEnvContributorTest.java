/*
 * The MIT License
 *
 * Copyright (c) 2014 Red Hat, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.jenkinsci.plugins.dockerbuildstep;

import static org.junit.Assert.assertEquals;
import hudson.EnvVars;
import hudson.model.FreeStyleBuild;

import java.util.HashMap;
import java.util.Map;

import org.jenkinsci.plugins.dockerbuildstep.action.EnvInvisibleAction;
import org.jenkinsci.plugins.dockerbuildstep.action.ExecEnvInvisibleAction;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.InternetProtocol;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.Ports.Binding;

public class DockerEnvContributorTest {

    @Rule public JenkinsRule j = new JenkinsRule();

    private DockerEnvContributor contributor = new DockerEnvContributor();

    @Test
    public void doNotTouchExistingContainerIds() throws Exception {
        FreeStyleBuild build = j.createFreeStyleProject().scheduleBuild2(0).get();

        final EnvVars envVars = existingEnvVars("existing,ids");

        contributor.buildEnvironmentFor(build, envVars, null);

        assertEquals("existing,ids", envVars.get(contributor.CONTAINER_IDS_ENV_VAR));
    }

    @Test
    public void addNewContainerIds() throws Exception {
        FreeStyleBuild build = j.createFreeStyleProject().scheduleBuild2(0).get();

        final EnvVars envVars = new EnvVars();
        build.addAction(contributedEnvVars("new,ids"));

        contributor.buildEnvironmentFor(build, envVars, null);

        assertEquals("new,ids", envVars.get(contributor.CONTAINER_IDS_ENV_VAR));
    }

    @Test
    public void mergeContainerIds() throws Exception {
        FreeStyleBuild build = j.createFreeStyleProject().scheduleBuild2(0).get();

        final EnvVars envVars = existingEnvVars("original");
        build.addAction(contributedEnvVars("new"));

        contributor.buildEnvironmentFor(build, envVars, null);

        assertEquals("original,new", envVars.get(contributor.CONTAINER_IDS_ENV_VAR));
    }

    @Test
    public void collapseSameContainerIds() throws Exception {
        FreeStyleBuild build = j.createFreeStyleProject().scheduleBuild2(0).get();

        final EnvVars envVars = existingEnvVars("existing,duplicate");

        build.addAction(contributedEnvVars("duplicate"));
        build.addAction(contributedEnvVars("new"));

        contributor.buildEnvironmentFor(build, envVars, null);

        assertEquals("existing,duplicate,new", envVars.get(contributor.CONTAINER_IDS_ENV_VAR));
    }

    @Test
    public void singleExecCommandSetsCommandId() throws Exception {
        FreeStyleBuild build = j.createFreeStyleProject().scheduleBuild2(0).get();

        final EnvVars envVars = new EnvVars();

        build.addAction(new ExecEnvInvisibleAction("test-container-id",
            createDummyExecCreateCmdResponse("exec-command-id")));

        contributor.buildEnvironmentFor(build, envVars, null);

        assertEquals(
            "exec-command-id",
            envVars.get(contributor.EXEC_COMMAND_ID_PREFIX
                + "test-container-id"));
    }

    @Test
    public void multipleExecCommandSetsAllCommandIds() throws Exception {
        FreeStyleBuild build = j.createFreeStyleProject().scheduleBuild2(0).get();

        final EnvVars envVars = new EnvVars();

        build.addAction(new ExecEnvInvisibleAction("test-container-id1",
            createDummyExecCreateCmdResponse("exec-command-id1")));
        build.addAction(new ExecEnvInvisibleAction("test-container-id2",
            createDummyExecCreateCmdResponse("exec-command-id2")));

        contributor.buildEnvironmentFor(build, envVars, null);

        assertEquals(
            "exec-command-id1",
            envVars.get(contributor.EXEC_COMMAND_ID_PREFIX
                + "test-container-id1"));
        assertEquals(
            "exec-command-id2",
            envVars.get(contributor.EXEC_COMMAND_ID_PREFIX
                + "test-container-id2"));
    }

    @Test
    public void portVariablesArePopulated() throws Exception {
        FreeStyleBuild build = j.createFreeStyleProject().scheduleBuild2(0).get();

        final EnvVars envVars = new EnvVars();

        build.addAction(contributedEnvVars("id"));

        contributor.buildEnvironmentFor(build, envVars, null);

        assertEquals("5432",
            envVars.get(contributor.PORT_BINDING_PREFIX + "TCP_1234"));
    }

    @Test
    public void portAndCommandVariablesArePopulated() throws Exception {
        FreeStyleBuild build = j.createFreeStyleProject().scheduleBuild2(0).get();

        final EnvVars envVars = new EnvVars();

        build.addAction(contributedEnvVars("id"));
    
        build.addAction(new ExecEnvInvisibleAction("test-container-id",
            createDummyExecCreateCmdResponse("exec-command-id")));

        contributor.buildEnvironmentFor(build, envVars, null);

        assertEquals("5432",
            envVars.get(contributor.PORT_BINDING_PREFIX + "TCP_1234"));
        assertEquals(
            "exec-command-id",
            envVars.get(contributor.EXEC_COMMAND_ID_PREFIX
                + "test-container-id"));
    }

    private EnvVars existingEnvVars(String ids) {
        return new EnvVars(contributor.CONTAINER_IDS_ENV_VAR, ids);
    }

    private EnvInvisibleAction contributedEnvVars(final String id) {
        return new EnvInvisibleAction() {
            @Override public String getId() {
                return id;
            }

            @Override public String getHostName() {
                return "";
            }

            @Override public String getIpAddress() {
                return "";
            }

            @Override public boolean hasPortBindings() {
                return true;
            }

            @Override public Map<ExposedPort, Binding[]> getPortBindings() {
                Ports.Binding[] bindings = new Ports.Binding[1];
                bindings[0] = new Ports.Binding(5432);

                Map<ExposedPort, Ports.Binding[]> portMap = new HashMap<ExposedPort, Ports.Binding[]>();
                portMap.put(new ExposedPort(1234,
                    InternetProtocol.TCP), bindings);
                return portMap;
            }
        };
    }

    private ExecCreateCmdResponse createDummyExecCreateCmdResponse(
        final String commandId) {
        return new ExecCreateCmdResponse() {
            @Override
            public String getId() {
                return commandId;
            }
        };
    }
}
