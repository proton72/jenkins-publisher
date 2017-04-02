package org.jenkinsci.plugins.publisherrunner;

import hudson.*;
import hudson.model.AbstractProject;
import hudson.model.Computer;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.tools.ToolInstallation;
import hudson.util.ArgumentListBuilder;
import hudson.util.FormValidation;
import jenkins.tasks.SimpleBuildStep;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Optional;

/**
 * Created by vladdenisov on 28.03.2017.
 */
public class ConfluencePublisherRunner extends Recorder implements SimpleBuildStep {

    private final String runnable;
    private final String allureReportUrl;
    private final String artifactsUrl;

    @DataBoundConstructor
    public ConfluencePublisherRunner(String runnable, String allureReportUrl, String artifactsUrl) {
        this.runnable = runnable;
        this.allureReportUrl = allureReportUrl;
        this.artifactsUrl = artifactsUrl;
    }

    public PublisherInstallation getInstallation() {
        if (runnable == null) return null;
        Optional<PublisherInstallation> publisherInstallation = Arrays.stream(RECORDER_DESCRIPTOR.getInstallations())
                .filter(i -> runnable.equals(i.getName()))
                .findFirst();
        return publisherInstallation.orElse(null);
    }

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath filePath, @Nonnull Launcher launcher, @Nonnull TaskListener taskListener)
            throws InterruptedException, IOException {
        PrintStream currentLogger = taskListener.getLogger();

        PublisherInstallation publisher = getInstallation();
        if (publisher == null) {
            taskListener.fatalError("Publisher is not found");
            throw new RuntimeException("Publisher is not found");
        }
        publisher.forNode(Computer.currentComputer().getNode(), taskListener);

        EnvVars buildCommonVariables = run.getEnvironment(taskListener);
        publisher.forEnvironment(buildCommonVariables);

        String host = buildCommonVariables.get("host");
        String buildUrl = buildCommonVariables.getOrDefault("BUILD_URL", "");


        String pathToRunner = publisher.getHome();
        FilePath exec = new FilePath(launcher.getChannel(), pathToRunner);

        try {
            if (!exec.exists()) {
                taskListener.fatalError("Path to publisher file is not found " + pathToRunner);
                throw new RuntimeException("Path to publisher file is not found " + pathToRunner);
            }
        } catch (IOException e) {
            taskListener.fatalError("IOException during publisher check: " + pathToRunner);
            throw new RuntimeException("IOException during publisher check: " + pathToRunner);
        }

        ArgumentListBuilder execBuilder = new ArgumentListBuilder();
        execBuilder.add(pathToRunner, publisher.getConfigHome(), buildUrl + getAllureReportUrl(),
                buildUrl + getArtifactsUrl()
        );

        if (host == null) {
            taskListener.error("Host of the test portal is not defined");
        } else {
            execBuilder.add(host);
        }

        try {
            Proc proc = launcher.launch().cmds(execBuilder).envs(buildCommonVariables).stdout(taskListener).pwd(filePath).start();
            int exitCode = proc.join();
            currentLogger.printf("Confluence publisher exit code is: %d\n", exitCode);
        } catch (IOException e) {
            e.printStackTrace();
            currentLogger.println("IOException during publishing!");
        } catch (InterruptedException e) {
            e.printStackTrace();
            currentLogger.println("InterruptedException during publishing!");
        }


    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    public String getRunnable() {
        return runnable;
    }

    public String getAllureReportUrl() {
        return allureReportUrl;
    }

    public String getArtifactsUrl() {
        return artifactsUrl;
    }

    public BuildStepDescriptor<Publisher> getDescriptor() {
        return RECORDER_DESCRIPTOR;
    }

    @Extension
    public static final DescriptorImpl RECORDER_DESCRIPTOR = new DescriptorImpl();

    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        @CopyOnWrite
        private volatile PublisherInstallation[] publishers = new PublisherInstallation[0];

        public DescriptorImpl() {
            super(ConfluencePublisherRunner.class);
            load();
        }

        public PublisherInstallation[] getInstallations() {
            return publishers;
        }

        public void setInstallations(PublisherInstallation... publishers) {
            this.publishers = publishers;
            save();
        }

        public FormValidation doCheckRunnableConfigFile(@QueryParameter("runnable") String value)
                throws IOException, ServletException {
            return ValidationUtils.validateString(value);
        }

        public FormValidation doCheckAllureReportUrl(@QueryParameter("allureReportUrl") String value)
                throws IOException, ServletException {
            return ValidationUtils.validateString(value);
        }

        public FormValidation doCheckArtifactsUrl(@QueryParameter("artifactsUrl") String value)
                throws IOException, ServletException {
            return ValidationUtils.validateString(value);
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types
            return true;
        }

        public PublisherInstallation.DescriptorImpl getToolDescriptor() {
            return ToolInstallation.all().get(PublisherInstallation.DescriptorImpl.class);
        }


        public String getDisplayName() {
            return Messages.PublisherRunner_DisplayName();
        }
    }
}
