/**
 * Copyright 2012 NetDigital Sweden AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package com.nginious.http.application;

import java.io.File;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.nginious.http.common.FileUtils;
import com.nginious.http.server.HttpServer;
import com.nginious.http.server.HttpServerConfiguration;
import com.nginious.http.server.HttpServerFactory;
import com.nginious.http.server.HttpTestConnection;

public class Http11ServiceTestCase extends TestCase {
	
	private HttpServer server;
	
	private File tmpDir;
	
    public Http11ServiceTestCase() {
		super();
	}

	public Http11ServiceTestCase(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
		this.tmpDir = new File(System.getProperty("java.io.tmpdir"), "webapps");
		tmpDir.mkdir();
		File destFile = new File(this.tmpDir, "test.war");
		FileUtils.copyFile("build/libs/nginious-server-0.9.2-testweb.war", destFile.getAbsolutePath());
		HttpServerConfiguration config = new HttpServerConfiguration();
		config.setWebappsDir(tmpDir.getAbsolutePath());
		config.setServerLogPath("build/test-server.log");
		config.setAccessLogPath("build/test-access.log");
		config.setPort(9000);
		HttpServerFactory factory = HttpServerFactory.getInstance();
		this.server = factory.create(config);
		server.start();
	}

	protected void tearDown() throws Exception {
		if(this.server != null) {
			server.stop();
		}

		FileUtils.deleteDir(this.tmpDir);
	}
	
	public void testServiceGet() throws Exception {
		String request = "GET /test/servicetest HTTP/1.1\015\012" + 
			"Host: localhost\015\012" +
			"Content-Length: 0\015\012" + 
			"Connection: close\015\012\015\012";
		
		String expectedResponse = "HTTP/1.1 200 OK\015\012" +
			"Content-Type: text/plain; charset=utf-8\015\012" +
			"Date: <date>\015\012" + 
			"Content-Length: 2\015\012" +
			"Connection: close\015\012" +
			"Server: Nginious/1.0.0\015\012\015\012" +
			"1\n";
		
		HttpTestConnection conn = null;
		
		try {
			conn = new HttpTestConnection();
			conn.write(request);
			
			String response = conn.readString();
			expectedResponse = conn.setHeaders(response, expectedResponse);
			assertEquals(expectedResponse, response);			
		} finally {
			if(conn != null) {
				conn.close();
			}
		}
		
		Thread.sleep(1500L);
		
		request = "GET /test/servicetest HTTP/1.1\015\012" + 
			"Host: localhost\015\012" +
			"Content-Length: 0\015\012" + 
			"Connection: close\015\012\015\012";
		
		expectedResponse = "HTTP/1.1 200 OK\015\012" +
			"Content-Type: text/plain; charset=utf-8\015\012" +
			"Date: <date>\015\012" + 
			"Content-Length: 2\015\012" +
			"Connection: close\015\012" +
			"Server: Nginious/1.0.0\015\012\015\012" +
			"2\n";
		
		conn = null;
		
		try {
			conn = new HttpTestConnection();
			conn.write(request);
			
			String response = conn.readString();
			expectedResponse = conn.setHeaders(response, expectedResponse);
			assertEquals(expectedResponse, response);			
		} finally {
			if(conn != null) {
				conn.close();
			}
		}		
	}
	
	public static Test suite() {
		return new TestSuite(Http11ServiceTestCase.class);
	}

	public static void main(String[] argv) {
		junit.textui.TestRunner.run(suite());
	}
}
