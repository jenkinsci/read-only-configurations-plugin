package org.jenkinsci.plugins.readonly;

import hudson.Functions;
import hudson.model.Job;
import hudson.model.User;
import org.kohsuke.stapler.Stapler;
import org.xml.sax.SAXException;

import javax.servlet.ServletException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.IOException;

/**
 * Transform html formular code to read-only
 * 
 * @author Lucie Votypkova
 */
public class ReadOnlyUtil {


    public static String transformInputsToReadOnly(String source, String taskUrl) throws IOException, SAXException, ParserConfigurationException, TransformerConfigurationException, TransformerException {
        
        StringBuffer buffer = new StringBuffer(source);
        int position=0;
        int divTaskCount =0;
        int divIncludedCount=0;
        while (true) {
            position = buffer.indexOf("<", position);
            if (position == -1) {
                break;
            }
            int end = buffer.indexOf(">", position);
            String tag = buffer.substring(position, end);
            if(tag.startsWith("<iframe")){
                position = handleIFrames(buffer, position);
                continue;
            }
            if (tag.startsWith("<input")) {
                String replacement = readonlyInput(tag);
                buffer.replace(position, end, replacement);
            }
            if (tag.startsWith("<textarea")) {
                String replacement = readonlyTextArea(tag);
                buffer.replace(position, end, replacement);
            }
            if (tag.startsWith("<option")) {
                String replacement = readonlyOption(tag);
                buffer.replace(position, end, replacement);
            }
            if(taskUrl!=null && tag.startsWith("<div")){
                if(tag.contains("class=\"task\"")){
                    divTaskCount++;
                }
                else{
                    if(divTaskCount>0)
                        divIncludedCount++;
                }
            }
            if(divTaskCount>0 && tag.startsWith("<a href")){
                int hrefValueStart = buffer.indexOf("\"", position)+1;
                int hrefValueEnd = buffer.indexOf("\"", hrefValueStart);
                String hrefValue = buffer.substring(hrefValueStart, hrefValueEnd);
                if(!(hrefValue.startsWith(taskUrl) || hrefValue.startsWith(Stapler.getCurrentRequest().getRootPath())))
                    buffer.replace(hrefValueStart, hrefValueEnd, taskUrl + hrefValue);
            }
            if(tag.startsWith("<\\div")){
                if(divIncludedCount>0){
                    divIncludedCount--;
                }
                else{
                   divTaskCount--; 
                }
                    
            }
            position=end;
        }
        return buffer.toString();
    }
    
    
    protected static int handleIFrames(StringBuffer buffer, int startFrame){
        int endFrame = buffer.indexOf("</iframe>",startFrame) + 9;
        buffer.replace(startFrame, endFrame, "");
        return endFrame;
    }

    
    public static String readonlyInput(String tag) {
        if (!(tag.matches("(.*)[class=\"]([^\"]*)advancedButton(.*)") || tag.contains("id=\"search-box\""))) {
            if (tag.contains("type=\"text\"")) {
                tag = tag.replace("<input", "<input readonly=\"readonly\"");
            } else { 
                tag = tag.replace("<input", "<input disabled=\"disabled\" readonly=\"readonly\""); //there is a lot of types which is not text
                if(tag.contains("type=\"password\"")){
                    String attributes[] = tag.split(" ");
                    for(String attribute: attributes){
                        if(attribute.contains("value=\""))
                            tag = tag.replace(attribute, "value=\"*****\"");
                    }
                }
            }
        }
        return tag;
    }

    public static String readonlyTextArea(String tag) {
        if (tag.contains("codemirror")) {
            int position = 0;
            StringBuffer buffer = new StringBuffer(tag);
            while(true){
                position = buffer.indexOf("codemirror", position);
                if (position == -1) {
                    break;
                }
                int end = buffer.indexOf("\"", position);
                end = buffer.indexOf("\"", end);
                buffer.replace(position, end, "");
                tag = buffer.toString();
            }
        }
        tag = tag.replace("<textarea", "<textarea readonly=\"readonly\"");

        return tag;
    }

    public static String readonlyOption(String tag) {
        tag = tag.replace("<option", "<option disabled=\"disabled\"");
        return tag;
    }
    
    public static boolean isAvailableJobConfiguration(Job target) throws IOException, ServletException{
        User user = User.current();
        if(user==null || user.getProperty(UserConfiguration.class).getDisplayForReadOnlyPermission()){
            return (!(Functions.hasPermission(target, target.CONFIGURE) || Functions.hasPermission(target, target.EXTENDED_READ))) && Functions.hasPermission(target, target.READ);
        }
        else{
            return true;
        }
    }
}
