package dmitrygusev.ping.services;

import org.junit.Assert;
import org.junit.Test;

import com.google.appengine.api.labs.taskqueue.TaskOptions;

import dmitrygusev.ping.filters.RunJobFilter;

public class TestApplication {

    @Test
    public void test() throws Exception {
        Application application = new Application(null, null, null, null, null, null, null, null, null);
        TaskOptions task = application.buildTaskUrl(RunJobFilter.class);
        Assert.assertEquals("/filters/runJob/", task.getUrl());
    }
}
