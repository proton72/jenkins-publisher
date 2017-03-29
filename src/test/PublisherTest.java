import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import org.apache.commons.io.FileUtils;
import org.jenkinsci.plugins.publisherrunner.ConfluencePublisherRunner;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

/**
 * Created by vladdenisov on 28.03.2017.
 */
public class PublisherTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Test
    public void publishResult() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.getPublishersList().add(new ConfluencePublisherRunner("publisher.jar", "publisher.conf", "allure", "artifact/target/allure-report/log"));
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        System.out.println(build.getDisplayName() + " completed");
        String log = FileUtils.readFileToString(build.getLogFile());
        System.out.println("Log is " + log);
    }
}
