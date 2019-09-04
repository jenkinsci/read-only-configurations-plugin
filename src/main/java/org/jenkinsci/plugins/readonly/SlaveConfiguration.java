/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jenkinsci.plugins.readonly;

import hudson.model.Action;
import hudson.model.Computer;
import hudson.security.Permission;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.jelly.Script;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.jelly.DefaultScriptInvoker;
import org.kohsuke.stapler.jelly.HTMLWriterOutput;

/**
 * Read-only configuration for computers (@link Computer)
 *
 * @author Lucie Votypkova
 */
public class SlaveConfiguration implements Action {

    private Computer computer;

    public SlaveConfiguration(Computer computer) {
        this.computer = computer;
    }

    public String getIconFileName() {
        return "search.png";
    }

    public String getDisplayName() {
        return "Slave read-only configuration";
    }

    public String getUrlName() {
        return "configure-readonly";
    }

    /**
     * Determine if the read-only configuration is available for current user
     *
     * @return true if the current user does not have permission to configure computers (@link Computer)
     */
    public boolean isAvailable() {
        return !computer.hasPermission(Permission.CONFIGURE);
    }

    /**
     * Compile script with a context for Job class
     *
     * @return compiled script
     */
    public Script compileScript() {
        try {
            return ConfigurationUtil.compileScript(getConfigContent(), "org.jenkinsci.plugins.readonly.SlaveConfiguration");
        } catch (Exception ex) {
            Logger.getLogger(JobConfiguration.class.getName()).log(Level.WARNING, "Read-only configuration plugin failed to compile script", ex);
        }
        return null;
    }

    public String getConfigContent() {
        try {
            URL url = Computer.class.getResource("Computer/configure.jelly");
            String outputConfig = ConfigurationUtil.urlToString(url);
            return outputConfig.replace("it.CONFIGURE", "it.READ"); //change permission 
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Read-only configuration plugin failed to load configuration script", e);
            return null;
        }
    }

    public void doIndex(StaplerRequest request, StaplerResponse response) throws IOException {
        transformToReadOnly(request, response);
    }

    public void transformToReadOnly(StaplerRequest request, StaplerResponse response) throws IOException {
        try {
            Object it = computer;
            Script configScript = compileScript();
            String taskUrl = request.getContextPath() + "/" + computer.getUrl();
            ConfigurationUtil.transformToReadOnly(request, response, configScript, it, taskUrl);

        } catch (Exception ex) {
            ex.printStackTrace(new PrintStream(response.getOutputStream(), false, Settings.encoding));
        }
    }

}
