package org.jenkinsci.plugins.dockerbuildstep.action;

import hudson.model.InvisibleAction;

import org.jenkinsci.plugins.dockerbuildstep.DockerEnvContributor;
import org.jenkinsci.plugins.dockerbuildstep.cmd.ExecCreateCommand;

import com.github.dockerjava.api.command.ExecCreateCmdResponse;

/**
 * Helper invisible action which is used for exchanging information between
 * {@link ExecCreateCommand}s and other object like {@link DockerEnvContributor}
 * 
 */
public class ExecEnvInvisibleAction extends InvisibleAction {

    private String containerId;
    private ExecCreateCmdResponse execCreateCmdResponse;

    public ExecEnvInvisibleAction() {
    }

    public ExecEnvInvisibleAction(String containerId,
	    ExecCreateCmdResponse execCreateCmdResponse) {
	this.containerId = containerId;
	this.execCreateCmdResponse = execCreateCmdResponse;
    }

    public ExecCreateCmdResponse getExecCreateCmdResponse() {
	return execCreateCmdResponse;
    }

    public void setExecCreateCmdResponse(
	    ExecCreateCmdResponse execCreateCmdResponse) {
	this.execCreateCmdResponse = execCreateCmdResponse;
    }

    public String getContainerId() {
	return containerId;
    }

    public void setContainerId(String containerId) {
	this.containerId = containerId;
    }

    public String getCommandId() {
	return execCreateCmdResponse.getId();
    }
}