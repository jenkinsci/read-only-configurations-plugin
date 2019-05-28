package org.jenkinsci.plugins.readonly;

import hudson.model.Action;
import hudson.model.Job;
import jenkins.model.Jenkins;
import org.apache.commons.jelly.JellyContext;
import org.apache.commons.jelly.Script;
import org.kohsuke.stapler.MetaClass;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.WebApp;
import org.kohsuke.stapler.jelly.DefaultScriptInvoker;
import org.kohsuke.stapler.jelly.HTMLWriterOutput;
import org.kohsuke.stapler.jelly.JellyClassLoaderTearOff;
import org.xml.sax.InputSource;

import javax.servlet.ServletException;
import java.io.*;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

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
        Script result = null;
        try {
            MetaClass c = WebApp.getCurrent().getMetaClass(Jenkins.getInstance().getClass());
            JellyContext context = new JellyClassLoaderTearOff(c.classLoader).createContext();
            StringReader buffer = new StringReader(getConfigContent());
            InputSource source = new InputSource(buffer);
            source.setSystemId("org.jenkinsci.plugins.readonly.JobConfiguration");
            result = context.compileScript(source);
        } catch (Exception ex) {
            Logger.getLogger(JobConfiguration.class.getName()).log(Level.WARNING, "Read-only configuration plugin failed to compile script", ex);
        }
        return result;
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
    
    public boolean isAvailable() throws IOException, ServletException{
        return ReadOnlyUtil.isAvailableJobConfiguration(job);
    }
    
    public String getConfigContent(){
        try{
       URL url = Job.class.getResource("Job/configure.jelly");
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
    * 
    */
    public void transformToReadOnly(StaplerRequest request, StaplerResponse response) throws IOException {
            try {
                DefaultScriptInvoker invoker = new DefaultScriptInvoker();
                ByteArrayOutputStream out = new ByteArrayOutputStream(); 
                HTMLWriterOutput xmlOutput = HTMLWriterOutput.create(out);
                Script script = compileScript();           
                xmlOutput.useHTML(true);          
                invoker.invokeScript(request, response, script, job, xmlOutput);
                String page = ReadOnlyUtil.transformInputsToReadOnly(out.toString("UTF-8"), null);  
                OutputStream output = response.getCompressedOutputStream(request);
                output.write(page.getBytes());
                output.close();
            }
            catch(Exception ex){
                ex.printStackTrace(new PrintStream(response.getOutputStream()));
            }
    }
    
}
