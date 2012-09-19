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

package com.nginious.http.service;

import java.io.IOException;
import java.io.PrintWriter;

import com.nginious.http.HttpRequest;
import com.nginious.http.HttpResponse;
import com.nginious.http.HttpService;
import com.nginious.http.HttpServiceResult;
import com.nginious.http.HttpSession;
import com.nginious.http.application.Service;

@Service(path = "/memory")
@SuppressWarnings("unused")
public class TestMemorySessionService extends HttpService {
	
	public TestMemorySessionService() {
		super();
	}
	
	public HttpServiceResult executeGet(HttpRequest request, HttpResponse response) throws IOException {
		String operation = request.getParameter("operation");
		
		if(operation != null && operation.equals("create")) {
			HttpSession session = request.getSession(true);
			session.setAttribute("Test", "test");
			String data = (String)session.getAttribute("Test");
			response.setContentLength(data.length());
			response.setContentType("text/plain");
			response.setCharacterEncoding("utf-8");
			
			PrintWriter writer = response.getWriter();
			writer.print(data);
		}
		
		if(operation != null && operation.equals("change")) {
			HttpSession session = request.getSession(false);
			String data = "";
			
			if(session != null) {
				data = (String)session.getAttribute("Test");
			}
			
			data = data + "test";
			
			if(session != null) {
				session.setAttribute("Test", data);
			}
			
			response.setContentLength(data.length());
			response.setContentType("text/plain");
			response.setCharacterEncoding("utf-8");
			
			PrintWriter writer = response.getWriter();
			writer.print(data);
		}
		
		if(operation != null && operation.equals("delete")) {
			HttpSession session = request.getSession(false);
			String data = (String)session.getAttribute("Test");
			session.invalidate();
			data = data + "test";
			session.setAttribute("Test", data);
			response.setContentLength(data.length());
			response.setContentType("text/plain");
			response.setCharacterEncoding("utf-8");
			
			PrintWriter writer = response.getWriter();
			writer.print(data);
		}
		
		return HttpServiceResult.DONE;
	}

	public HttpServiceResult executePost(HttpRequest request, HttpResponse response) throws IOException {
		return HttpServiceResult.DONE;
	}
}
