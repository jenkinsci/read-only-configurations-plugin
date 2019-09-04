package org.jenkinsci.plugins.readonly;

import hudson.model.Action;
import hudson.model.Job;
import org.apache.commons.jelly.Script;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.jelly.DefaultScriptInvoker;
import org.kohsuke.stapler.jelly.HTMLWriterOutput;

import javax.servlet.ServletException;
import java.io.*;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.jenkinsci.plugins.readonly.ConfigurationUtil.urlToString;

/**
 * Display Job configuration page in read-only form
 *
 * @author Lucie Votypkova
 */
public class JobConfiguration implements Action {

    private Job<?, ?> job;

    public JobConfiguration(Job<?, ?> job) {
        this.job = job;
    }

    /**
     * Compile script with a context for Job class
     *
     * @return compiled script
     */
    public Script compileScript() {
        try {
            return ConfigurationUtil.compileScript(getConfigContent(), "org.jenkinsci.plugins.readonly.JobConfiguration");
        } catch (Exception ex) {
            Logger.getLogger(JobConfiguration.class.getName()).log(Level.WARNING, "Read-only configuration plugin failed to compile script", ex);
        }
        return null;
    }

    public String getIconFileName() {
        return "search.png";
    }

    public String getDisplayName() {
        return "Read-only job configuration";
    }

    public String getUrlName() {
        return "configure-readonly";
    }

    public boolean isAvailable() throws IOException, ServletException {
        return ReadOnlyUtil.isAvailableJobConfiguration(job);
    }

    public String getConfigContent() {
        try {
            URL url = Job.class.getResource("Job/configure.jelly");
            String outputConfig = urlToString(url);
            return outputConfig.replace("it.EXTENDED_READ", "it.READ"); //change permission 
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Read-only configuration plugin failed to load configuration script", e);
            return null;
        }
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
            Object it = job;
            ConfigurationUtil.transformToReadOnly(request, response, configScript, it, null);
        } catch (Exception ex) {
            ex.printStackTrace(new PrintStream(response.getOutputStream(), false, Settings.encoding));
        }
    }

}
