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

/**
 * Created by vladdenisov on 28.03.2017.
 */
public class ConfluencePublisherRunner extends Recorder implements SimpleBuildStep {

    private static final String PLUGIN_DISPLAY_NAME = "Опубликовать результаты сборки на wiki";
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

    public PublisherInstallation getInstallation() {
        if (runnable == null) return null;
        for (PublisherInstallation installation : RECORDER_DESCRIPTOR.getInstallations()) {
            if (runnable.equals(installation.getName())) {
                return installation;
            }
        }
        return null;
    }

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath filePath, @Nonnull Launcher launcher, @Nonnull TaskListener taskListener) throws InterruptedException, IOException {
        EnvVars buildCommonVariables = run.getEnvironment(taskListener);
        PrintStream currentLogger = taskListener.getLogger();
        currentLogger.println("Список переменных окружения: ");
        buildCommonVariables.forEach((k, v) -> currentLogger.printf("%s=%s\n", k, v));

        PublisherInstallation publisher = getInstallation();
        if (publisher == null) {
            taskListener.fatalError("не нашли паблишер");
            throw new RuntimeException("не нашли паблишер");
        }
        publisher.forNode(Computer.currentComputer().getNode(), taskListener);
        publisher.forEnvironment(buildCommonVariables);

        String pathToRunner = publisher.getHome();
        FilePath exec = new FilePath(launcher.getChannel(), pathToRunner);

        try {
            if (!exec.exists()) {
                taskListener.fatalError("не нашли путь к паблишеру " + pathToRunner);
                throw new RuntimeException("не нашли путь к паблишеру " + pathToRunner);
            }
        } catch (IOException e) {
            taskListener.fatalError("не удалось проверить путь к паблишеру " + pathToRunner);
            throw new RuntimeException("не удалось проверить путь к паблишеру " + pathToRunner);
        }

        String host = buildCommonVariables.get("host");
        String buildUrl = buildCommonVariables.getOrDefault("BUILD_URL", "");

        ArgumentListBuilder execBuilder = new ArgumentListBuilder();
        execBuilder.add(pathToRunner, getRunnableConfigFile(), buildUrl + getAllureReportUrl(),
                buildUrl + getArtifactsUrl(), host);

        try {
            Proc proc = launcher.launch().cmds(execBuilder).envs(buildCommonVariables).stdout(taskListener).pwd(filePath).start();
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

    public BuildStepDescriptor<Publisher> getDescriptor() {
        return RECORDER_DESCRIPTOR;
    }

    @Extension
    public static final DescriptorImpl RECORDER_DESCRIPTOR = new DescriptorImpl();

    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        @CopyOnWrite
        private volatile PublisherInstallation[] publishers = new PublisherInstallation[0];

        /**
         * In order to load the persisted global configuration, you have to
         * call load() in the constructor.
         */
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
            if (value.length() == 0)
                return FormValidation.error("Пожалуйста, введите путь к конфигурационному файлу паблишера");
            if (value.length() < 4)
                return FormValidation.warning("Не слишком ли короткий путь?");
            return FormValidation.ok();
        }

        public FormValidation doCheckRunnable(@QueryParameter("runnableConfigFile") String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("Пожалуйста, введите путь к файлу паблишера");
            if (value.length() < 4)
                return FormValidation.warning("Не слишком ли короткий путь?");
            return FormValidation.ok();
        }

        public FormValidation doCheckAllureReportUrl(@QueryParameter("allureReportUrl") String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("Пожалуйста, введите относительный url allure отчета. Например, allure");
            if (value.length() < 4)
                return FormValidation.warning("Не слишком ли короткий путь?");
            return FormValidation.ok();
        }

        public FormValidation doCheckArtifactsUrl(@QueryParameter("artifactsUrl") String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("Пожалуйста, введите относительный url к логам в аритфактах билда. Например, target/allure-report/logs");
            if (value.length() < 4)
                return FormValidation.warning("Не слишком ли короткий путь?");
            return FormValidation.ok();
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types
            return true;
        }

        public PublisherInstallation.DescriptorImpl getToolDescriptor() {
            return ToolInstallation.all().get(PublisherInstallation.DescriptorImpl.class);
        }


        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return PLUGIN_DISPLAY_NAME;
        }
    }
}
