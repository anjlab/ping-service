package com.anjlab.ping.entities;

import java.util.regex.Pattern;

import junit.framework.Assert;

import org.junit.Test;

import com.anjlab.gae.QuotaDetails;
import com.google.appengine.api.quota.QuotaService;
import com.google.appengine.api.quota.QuotaServiceFactory;

public class TestQuotas {

    @Test
    public void convertMegacyclesToSeconds() {
        QuotaService qs = QuotaServiceFactory.getQuotaService();
        System.out.println(qs.convertMegacyclesToCpuSeconds(620));
    }
    
    @Test
    public void testQuotaLimited() {
        Assert.assertTrue(
                Pattern.matches(QuotaDetails.DATASTORE_WRITE_EXCEPTION,
                        "The API call datastore_v3.Put() required more quota than is available."));
    }
}
