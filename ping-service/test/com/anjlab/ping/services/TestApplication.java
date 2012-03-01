package com.anjlab.ping.services;

import org.junit.Assert;
import org.junit.Test;

import com.anjlab.ping.filters.RunJobFilter;
import com.anjlab.ping.services.Application;
import com.google.appengine.api.taskqueue.TaskOptions;


public class TestApplication {

    @Test
    public void testBuildTaskURL() throws Exception {
        Application application = new Application(null, null, null, null, null, null, null, null, null, null);
        TaskOptions task = application.buildTaskUrl(RunJobFilter.class);
        Assert.assertEquals("/filters/runJob/", task.getUrl());
    }
}
