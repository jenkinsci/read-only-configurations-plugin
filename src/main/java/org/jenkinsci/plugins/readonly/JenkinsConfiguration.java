package org.jenkinsci.plugins.readonly;

import hudson.Extension;
import hudson.model.RootAction;
import jenkins.model.Jenkins;
import org.apache.commons.jelly.Script;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Display Jenkins configuration page in read-only form
 *
 * @author Lucie Votypkova
 */
@Extension
public class JenkinsConfiguration implements RootAction {

    private String configFileContent;
    private Logger log = Logger.getLogger(JenkinsConfiguration.class.getName());

    public JenkinsConfiguration() {
        try {
            URL url = Jenkins.class.getResource("Jenkins/configure.jelly");
            InputStream input = url.openConnection().getInputStream();
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            int b = 0;
            while (b != -1) {
                b = input.read();
                if (b != -1) {
                    output.write(b);
                }
            }
            String outputConfig = output.toString(Settings.encoding);
            configFileContent = outputConfig.replace("it.ADMINISTER", "it.READ"); //change permission
        } catch (Exception e) {
            log.log(Level.WARNING, "Read-only configuration plugin failed to load configuration script", e);
        }
    }

    /**
     * Compile script with a context for Jenkins class
     *
     * @return compiled script
     */
    public Script compileScript() {
        try {
            return ConfigurationUtil.compileScript(configFileContent, "org.jenkinsci.plugins.readonly.JenkinsConfiguration");
        } catch (Exception ex) {
            log.log(Level.WARNING, "Read-only configuration plugin failed to compile script", ex);
        }
        return null;
    }

    public String getIconFileName() {
        return "search.png";
    }

    public String getDisplayName() {
        return "Read-only global configuration";
    }

    public String getUrlName() {
        return "configure-readonly";
    }

    public void doIndex(StaplerRequest request, StaplerResponse response) throws IOException {
        transformToReadOnly(request, response);
    }

    /**
     * Transformation of html code which modify all formular's items to read-only
     */
    public void transformToReadOnly(StaplerRequest request, StaplerResponse response) throws IOException {
        try {
            Script configScript = compileScript();
            Object it = Jenkins.getInstance();
            ConfigurationUtil.transformToReadOnly(request, response, configScript, it, null);
        } catch (Exception ex) {
            ex.printStackTrace(new PrintStream(response.getOutputStream(), false, Settings.encoding));
        }
    }
}
