package org.jenkinsci.plugins.readonly;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jenkins.model.Jenkins;
import org.apache.commons.jelly.JellyContext;
import org.apache.commons.jelly.JellyException;
import org.apache.commons.jelly.JellyTagException;
import org.apache.commons.jelly.Script;
import org.kohsuke.stapler.MetaClass;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.WebApp;
import org.kohsuke.stapler.jelly.DefaultScriptInvoker;
import org.kohsuke.stapler.jelly.HTMLWriterOutput;
import org.kohsuke.stapler.jelly.JellyClassLoaderTearOff;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.*;
import java.net.URL;
import java.util.Objects;

public class ConfigurationUtil {
    static String urlToString(URL url) throws IOException {
        InputStream input = url.openConnection().getInputStream();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        int b = 0;
        while (b != -1) {
            b = input.read();
            if (b != -1) {
                output.write(b);
            }
        }
        return output.toString(Settings.encoding);
    }

    @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
    static Script compileScript(String configFileContent, String pluginName) throws JellyException {
        MetaClass c = WebApp.getCurrent().getMetaClass(Objects.requireNonNull(Jenkins.getInstance()).getClass());
        JellyContext context = new JellyClassLoaderTearOff(c.classLoader).createContext();
        StringReader buffer = new StringReader(configFileContent);
        InputSource source = new InputSource(buffer);
        source.setSystemId(pluginName);
        return context.compileScript(source);
    }

    static void transformToReadOnly(StaplerRequest request, StaplerResponse response, Script script, Object it, String taskUrl) throws IOException, JellyTagException, TransformerException, SAXException, ParserConfigurationException {
        DefaultScriptInvoker invoker = new DefaultScriptInvoker();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        HTMLWriterOutput xmlOutput = HTMLWriterOutput.create(output);
        xmlOutput.useHTML(true);
        invoker.invokeScript(request, response, script, it, xmlOutput);
        String page = ReadOnlyUtil.transformInputsToReadOnly(output.toString(Settings.encoding), taskUrl);
        OutputStream st = response.getCompressedOutputStream(request);
        st.write(page.getBytes(Settings.encoding));
        st.close();
    }

}
