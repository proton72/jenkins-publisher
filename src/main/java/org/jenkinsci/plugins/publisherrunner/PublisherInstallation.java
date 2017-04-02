package org.jenkinsci.plugins.publisherrunner;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.EnvironmentSpecific;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.slaves.NodeSpecific;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.servlet.ServletException;
import java.io.IOException;

/**
 * Created by vladdenisov on 29.03.2017.
 */
public final class PublisherInstallation extends ToolInstallation implements EnvironmentSpecific<PublisherInstallation>, NodeSpecific<PublisherInstallation> {

    private String configHome;

    @DataBoundConstructor
    public PublisherInstallation(String name, String home, String configHome) {
        super(name, home, null);
        this.configHome = configHome;
    }

    @Override
    public PublisherInstallation forNode(Node node, TaskListener taskListener) throws IOException, InterruptedException {
        return new PublisherInstallation(getName(), translateFor(node, taskListener), getConfigHome());
    }

    @Override
    public PublisherInstallation forEnvironment(EnvVars envVars) {
        return new PublisherInstallation(getName(), envVars.expand(getHome()), getConfigHome());
    }

    public String getConfigHome() {
        return configHome;
    }

    @Extension
    public static class DescriptorImpl extends ToolDescriptor<PublisherInstallation> {

        public String getDisplayName() {
            return Messages.PublisherInstallation_DisplayName();
        }

        @Override
        public PublisherInstallation[] getInstallations() {
            return Jenkins.getInstance()
                    .getDescriptorByType(ConfluencePublisherRunner.DescriptorImpl.class)
                    .getInstallations();
        }

        @Override
        public void setInstallations(PublisherInstallation... installations) {
            Jenkins.getInstance()
                    .getDescriptorByType(ConfluencePublisherRunner.DescriptorImpl.class)
                    .setInstallations(installations);
        }

        public FormValidation doCheckConfigHome(@QueryParameter("configHome") String value)
                throws IOException, ServletException {
             return ValidationUtils.validateString(value);
        }

    }
}
