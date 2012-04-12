package com.anjlab.ping.pages;

import java.security.Principal;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.tapestry5.dom.Document;
import org.apache.tapestry5.services.RequestGlobals;
import org.apache.tapestry5.test.PageTester;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;

public class TestPages {

    private final LocalServiceTestHelper helper =
            new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

    @Before
    public void setUp() {
        System.setProperty("appengine.orm.disable.duplicate.emf.exception", "false");
        helper.setUp();
    }

    @After
    public void tearDown() {
        helper.tearDown();
    }
        
    @Test
    public void testWelcomePage() throws Exception {
        PageTester pageTester = new PageTester("com.anjlab.ping", "App", "war");
        
        RequestGlobals requestGlobals = pageTester.getRegistry().getService(RequestGlobals.class);
        
        HttpServletRequest servletRequest = EasyMock.createMock(HttpServletRequest.class);
        servletRequest.setCharacterEncoding("UTF-8");
        EasyMock.expectLastCall().anyTimes();
        EasyMock.expect(servletRequest.getUserPrincipal()).andReturn(new Principal() {
            public String getName() {
                return "test@example.org";
            }
        }).anyTimes();
        
        HttpSession httpSession = EasyMock.createMock(HttpSession.class);
        EasyMock.expect(servletRequest.getSession()).andReturn(httpSession);
        
        ServletContext servletContext = EasyMock.createMock(ServletContext.class);
        EasyMock.expect(httpSession.getServletContext()).andReturn(servletContext);
        EasyMock.expect(servletContext.getRealPath("")).andReturn(null);
        
        EasyMock.replay(servletRequest);
        
        HttpServletResponse servletResponse = EasyMock.createMock(HttpServletResponse.class);
        requestGlobals.storeServletRequestResponse(servletRequest, servletResponse);
        
        Document document = pageTester.renderPage("Welcome");
        
        Assert.assertNotNull(document);
        Assert.assertTrue(!document.toString().contains("Application Exception"));
    }
}
