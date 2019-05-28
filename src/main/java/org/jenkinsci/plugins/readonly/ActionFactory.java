package org.jenkinsci.plugins.readonly;

import hudson.Extension;
import hudson.model.Action;
import hudson.model.Job;
import jenkins.model.TransientActionFactory;

import java.util.ArrayList;
import java.util.Collection;


/**
 *
 * @author Lucie Votypkova
 */
@Extension
public class ActionFactory extends TransientActionFactory<Job> {


    @Override
    public Class<Job> type() {
        return Job.class;
    }

    @Override
    public Collection<? extends Action> createFor(@SuppressWarnings("unchecked") Job target) {
        final ArrayList<Action> actions = new ArrayList<Action>();     
        final JobConfiguration newAction = new JobConfiguration(target);
        actions.add(newAction);              
        return actions;
    }
    
   

}

