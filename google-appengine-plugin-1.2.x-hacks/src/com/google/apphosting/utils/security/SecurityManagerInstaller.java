package com.google.apphosting.utils.security;

import java.net.URL;

public class SecurityManagerInstaller {

	public static void install(URL... urls) {
		System.out.println("Skip install SecurityManager");
	}
	
}
