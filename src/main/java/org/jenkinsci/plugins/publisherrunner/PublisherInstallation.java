package org.jenkinsci.plugins.publisherrunner;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.EnvironmentSpecific;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.slaves.NodeSpecific;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolProperty;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.List;

/**
 * Created by vladdenisov on 29.03.2017.
 */
public final class PublisherInstallation extends ToolInstallation implements EnvironmentSpecific<PublisherInstallation>, NodeSpecific<PublisherInstallation> {

    @DataBoundConstructor
    public PublisherInstallation(String name, String home, List<? extends ToolProperty<?>> properties) {
        super(name, home, properties);
    }

    @Override
    public PublisherInstallation forNode(Node node, TaskListener taskListener) throws IOException, InterruptedException {
        return new PublisherInstallation(getName(), translateFor(node, taskListener), getProperties());
    }

    @Override
    public PublisherInstallation forEnvironment(EnvVars envVars) {
        return new PublisherInstallation(getName(), envVars.expand(getHome()), getProperties());
    }

    @Extension
    public static class DescriptorImpl extends ToolDescriptor<PublisherInstallation> {

        public String getDisplayName() {
            return "Wiki publisher executable";
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

    }
}
