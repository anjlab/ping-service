package com.anjlab.ping.integration;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.server.RemoteControlConfiguration;
import org.openqa.selenium.server.SeleniumServer;

import com.anjlab.tapestry5.StaticAssetPathConverter;
import com.thoughtworks.selenium.CommandProcessor;
import com.thoughtworks.selenium.DefaultSelenium;
import com.thoughtworks.selenium.HttpCommandProcessor;
import com.thoughtworks.selenium.Selenium;

public class CreateStaticAssets {

    private static SeleniumServer seleniumServer;
    private static Selenium selenium;
    
    @BeforeClass
    public static void startUp() throws Exception {
        String baseURL = "http://localhost:8888";

        seleniumServer = new SeleniumServer();
        seleniumServer.start();
        
        CommandProcessor httpCommandProcessor =
                new HttpCommandProcessor("localhost", 
                        RemoteControlConfiguration.DEFAULT_PORT, "*googlechrome", baseURL);

        selenium = new DefaultSelenium(httpCommandProcessor);
        selenium.start();
    }
    
    @AfterClass
    public static void tearDown() {
        selenium.stop();
        seleniumServer.stop();
    }
    
    @Test
    public void precompileAssets() {
        //  XXX set production = true in AppModule
        //  XXX Run application with system property
        //  -D--enable_all_permissions=true
        selenium.addCustomRequestHeader(StaticAssetPathConverter.ASSETS_PRECOMPILATION, "true");
        
        selenium.open("/");
        Assert.assertEquals("Welcome to Ping Service", selenium.getTitle());
        
        selenium.click("//a");
        selenium.waitForPageToLoad("60000");
        Assert.assertEquals("", selenium.getTitle());
        
        selenium.type("id=email", "test@example.com");
        selenium.check("id=isAdmin");
        selenium.click("//input[@value='Log In']");
        selenium.waitForPageToLoad("60000");
        Assert.assertEquals("Ping Service", selenium.getTitle());
        
        selenium.click("//a[text()='Edit']");
        selenium.waitForPageToLoad("60000");
        Assert.assertEquals("Edit Job - Ping Service", selenium.getTitle());
        
        selenium.open("/");
        Assert.assertEquals("Ping Service", selenium.getTitle());
        
        selenium.click("//a[text()='Analyze']");
        selenium.waitForPageToLoad("60000");
        Assert.assertEquals("Job Analytics - Ping Service", selenium.getTitle());
        
        selenium.open("/help");
        Assert.assertEquals("Help - Ping Service", selenium.getTitle());
        
        selenium.open("/feedback");
        Assert.assertEquals("Feedback - Ping Service", selenium.getTitle());
        
        selenium.open("/settings");
        Assert.assertEquals("Settings - Ping Service", selenium.getTitle());
        
        Pattern appVersionPattern = Pattern.compile(".*/assets/([^/]+)/", Pattern.DOTALL);
        String htmlSource = selenium.getHtmlSource();
        Matcher matcher = appVersionPattern.matcher(htmlSource);
        Assert.assertTrue(matcher.find());
        String appVersion = matcher.group(1);
        
        Assert.assertNotNull(appVersion);
        
        selenium.open("/assets/" + appVersion + "/ctx/images/exclamation.png");
        selenium.open("/assets/" + appVersion + "/ctx/images/tick.png");
        selenium.open("/assets/" + appVersion + "/ctx/images/bullet_error.png");
        selenium.open("/assets/" + appVersion + "/ctx/images/external.png");
        selenium.open("/assets/" + appVersion + "/ctx/images/ping-service.png");
        selenium.open("/assets/" + appVersion + "/ctx/images/analytics-demo.png");
    }
}
