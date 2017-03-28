package org.jenkinsci.plugins.publisherrunner;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Proc;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import jenkins.tasks.SimpleBuildStep;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.io.IOException;

/**
 * Created by vladdenisov on 28.03.2017.
 */
public class ConfluencePublisherRunner extends Notifier implements SimpleBuildStep {

    private final String runnable;
    private final String runnableConfigFile;
    private final String allureReportUrl;
    private final String artifactsUrl;

    @DataBoundConstructor
    public ConfluencePublisherRunner(String runnable, String runnableConfigFile, String allureReportUrl, String artifactsUrl) {
        this.runnable = runnable;
        this.runnableConfigFile = runnableConfigFile;
        this.allureReportUrl = allureReportUrl;
        this.artifactsUrl = artifactsUrl;
    }


    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath filePath, @Nonnull Launcher launcher, @Nonnull TaskListener taskListener) throws InterruptedException, IOException {
        EnvVars buildCommonVariables = run.getEnvironment(taskListener);
        String host = buildCommonVariables.get("host");
        String buildUrl = buildCommonVariables.get("BUILD_URL");

        try {
            Proc proc = launcher.launch(runnable, new String[]{runnableConfigFile, buildUrl + allureReportUrl, buildUrl + artifactsUrl, host}, taskListener.getLogger(), filePath);
            int exitCode = proc.join();
            taskListener.getLogger().printf("Confluence publisher exit code is: %d\n", exitCode);
        } catch (IOException e) {
            e.printStackTrace();
            taskListener.getLogger().println("IOException !");
        } catch (InterruptedException e) {
            e.printStackTrace();
            taskListener.getLogger().println("InterruptedException!");
        }


    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    public String getRunnable() {
        return runnable;
    }

    public String getRunnableConfigFile() {
        return runnableConfigFile;
    }

    public String getAllureReportUrl() {
        return allureReportUrl;
    }

    public String getArtifactsUrl() {
        return artifactsUrl;
    }

}
