package org.jenkinsci.plugins.readonly;

import hudson.Extension;
import hudson.model.RootAction;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.jelly.Script;
import org.kohsuke.stapler.MetaClass;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.WebApp;
import org.kohsuke.stapler.jelly.DefaultScriptInvoker;
import org.kohsuke.stapler.jelly.HTMLWriterOutput;
import org.kohsuke.stapler.jelly.JellyClassLoaderTearOff;
import jenkins.model.Jenkins;
import org.apache.commons.jelly.JellyContext;
import org.xml.sax.InputSource;

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
            String outputConfig = output.toString();
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
        Script result = null;
        try {
            MetaClass c = WebApp.getCurrent().getMetaClass(Jenkins.getInstance().getClass());
            JellyContext context = new JellyClassLoaderTearOff(c.classLoader).createContext();
            StringReader buffer = new StringReader(configFileContent);
            InputSource source = new InputSource(buffer);
            source.setSystemId(JenkinsConfiguration.class.getResource("JenkinsConfiguration").toString());
            result = context.compileScript(source);
        } catch (Exception ex) {
            log.log(Level.WARNING, "Read-only configuration plugin failed to compile script", ex);
        }
        return result;
    }

    public String getIconFileName() {
        return "search.png";
    }

    public String getDisplayName() {
        return "Global configuration";
    }

    public String getUrlName() {
        return "configure-readonly";
    }  
    
    public void doIndex(StaplerRequest request, StaplerResponse response) throws IOException{
        transformToReadOnly(request, response);        
    }
    
    /**
     * Transformation of html code which modify all formular's items to read-only
     * 
     */
    public void transformToReadOnly(StaplerRequest request, StaplerResponse response) throws IOException {
        try {
            Script configScript = compileScript();
            DefaultScriptInvoker invoker = new DefaultScriptInvoker();
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            HTMLWriterOutput xmlOutput = HTMLWriterOutput.create(output);
            xmlOutput.useHTML(true);
            invoker.invokeScript(request, response, configScript, Jenkins.getInstance(), xmlOutput);
            String page = ReadOnlyUtil.transformInputsToReadOnly(output.toString());
            OutputStream st = response.getCompressedOutputStream(request);
            st.write(page.getBytes());
            st.close();
        } catch (Exception ex) {
            ex.printStackTrace(new PrintStream(response.getOutputStream()));
        }
    }
}
