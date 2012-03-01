package com.anjlab.ping.services;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.tapestry5.ioc.Configuration;
import org.junit.Assert;
import org.junit.Test;

public class TestRoutines {

    @Test
    public void testIgnorePath()
    {
        final List<String> items = new ArrayList<String>();
        
        AppModule.contributeIgnoredPathsFilter(new Configuration<String>()
        {
            @Override
            public void addInstance(Class<? extends String> clazz) { }
            
            @Override
            public void add(String object) { items.add(object); }
        });
        
        String ah = items.get(0);
        
        Assert.assertFalse(Pattern.matches(ah, "/_ah/warmup"));
        Assert.assertTrue(Pattern.matches(ah, "/_ah/login?continue=%2F_ah%2Flogout"));
    }
}
