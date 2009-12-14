package com.google.appengine.tools.development;

import java.security.Permission;

public class DevAppServerFactory {

	public static class CustomSecurityManager extends SecurityManager {

		@Override
		public void checkPermission(Permission perm) {
			//	Do nothing
		}
		
        public CustomSecurityManager(DevAppServer devAppServer) {
            System.out.println("Create dummy CustomSecurityManager");
        }
        
	}
	
}
