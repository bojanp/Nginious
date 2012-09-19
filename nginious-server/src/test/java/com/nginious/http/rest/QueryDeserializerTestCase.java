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

package com.nginious.http.rest;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.nginious.http.HttpMethod;
import com.nginious.http.rest.Deserializer;
import com.nginious.http.rest.DeserializerFactory;
import com.nginious.http.rest.SerializerException;
import com.nginious.http.rest.SerializerFactoryException;
import com.nginious.http.server.HttpTestRequest;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class QueryDeserializerTestCase extends TestCase {
	
	public QueryDeserializerTestCase() {
		super();
	}

	public QueryDeserializerTestCase(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		
	}
	
	public void testQueryDeserialization() throws Exception {
		HttpTestRequest request = new HttpTestRequest();
		request.setMethod(HttpMethod.GET);
		request.addHeader("Content-Type", "application/x-www-form-urlencoded");
		request.addHeader("Accept", "text/xml");
		request.addParameter("first", "true");
		request.addParameter("second", "0.567");
		request.addParameter("third", "0.452");
		request.addParameter("fourth", "10");
		request.addParameter("fifth", "3400000000");
		request.addParameter("sixth", "32767");
		request.addParameter("seventh", "String");
		request.addParameter("eight", "2011-08-24T08:50:23+02:00");
		request.addParameter("ninth", "2011-08-24T08:52:23+02:00");
		
		Deserializer<InBean> deserializer = DeserializerFactory.getInstance().createDeserializer(InBean.class, "application/x-www-form-urlencoded");
		assertEquals("application/x-www-form-urlencoded", deserializer.getMimeType());
		InBean bean = deserializer.deserialize(request);
		
		assertEquals(true, bean.getFirst());
		assertEquals(0.567, bean.getSecond());
		assertEquals(0.452f, bean.getThird());
		assertEquals(10, bean.getFourth());
		assertEquals(3400000000L, bean.getFifth());
		assertEquals(32767, bean.getSixth());
		assertEquals("String", bean.getSeventh());
		assertEquals("2011-08-24T08:50:23+02:00", formatDate(bean.getEight()));
		assertEquals("2011-08-24T08:52:23+02:00", formatDate(bean.getNinth().getTime()));
	}
	
	public void testQueryDeserializationBadValues() throws Exception {
		HttpTestRequest request = new HttpTestRequest();
		request.setMethod(HttpMethod.GET);
		request.addHeader("Content-Type", "application/x-www-form-urlencoded");
		request.addHeader("Accept", "text/xml");
		request.addParameter("first", "true");
		request.addParameter("second", "0.567");
		request.addParameter("third", "0.452");
		request.addParameter("fourth", "10");
		request.addParameter("fifth", "3400000000");
		request.addParameter("sixth", "32767");
		request.addParameter("seventh", "String");
		request.addParameter("eight", "2011-08-24T08:50:23+02:00");
		request.addParameter("ninth", "2011-08-24T08:52:23+02:00");
		
		Deserializer<InBean> deserializer = DeserializerFactory.getInstance().createDeserializer(InBean.class, "application/x-www-form-urlencoded");
		
		request.setParameter("second", "X");
		
		try {
			deserializer.deserialize(request);
			fail("Must not be possible to deserialize with bad double value");
		} catch(SerializerException e) {
			request.setParameter("second", "0.567");
		}
		
		request.setParameter("third", "X");
		
		try {
			deserializer.deserialize(request);
			fail("Must not be possible to deserialize with bad float value");
		} catch(SerializerException e) {
			request.setParameter("third", "0.452");
		}
		
		request.setParameter("fourth", "X");
		
		try {
			deserializer.deserialize(request);
			fail("Must not be possible to deserialize with bad int value");
		} catch(SerializerException e) {
			request.setParameter("fourth", "10");
		}
		
		request.setParameter("fifth", "X");
		
		try {
			deserializer.deserialize(request);
			fail("Must not be possible to deserialize with bad long value");
		} catch(SerializerException e) {
			request.setParameter("fifth", "3400000000");
		}
		
		request.setParameter("sixth", "65535");
		
		try {
			deserializer.deserialize(request);
			fail("Must not be possible to deserialize with bad short value");
		} catch(SerializerException e) {
			request.setParameter("sixth", "32767");
		}		

		request.setParameter("eight", "X");
		
		try {
			deserializer.deserialize(request);
			fail("Must not be possible to deserialize with bad date value");
		} catch(SerializerException e) {
			request.setParameter("sixth", "2011-08-24T08:50:23+02:00");
		}
		
		request.setParameter("ninth", "X");
		
		try {
			deserializer.deserialize(request);
			fail("Must not be possible to deserialize with bad calendar value");
		} catch(SerializerException e) {
			request.setParameter("sixth", "2011-08-24T08:50:23+02:00");
		}		
	}
	
	public void testQueryDeserializationNullValues() throws Exception {
		HttpTestRequest request = new HttpTestRequest();
		request.setMethod(HttpMethod.GET);
		request.addHeader("Content-Type", "application/x-www-form-urlencoded");
		request.addHeader("Accept", "text/xml");
		
		Deserializer<InBean> deserializer = DeserializerFactory.getInstance().createDeserializer(InBean.class, "application/x-www-form-urlencoded");
		InBean bean = deserializer.deserialize(request);
		
		assertNotNull(bean);
		assertEquals(false, bean.getFirst());
		assertEquals(0.0d, bean.getSecond());
		assertEquals(0.0f, bean.getThird());
		assertEquals(0, bean.getFourth());
		assertEquals(0L, bean.getFifth());
		assertEquals(0, bean.getSixth());
		assertNull(bean.getSeventh());
		assertNull(bean.getEight());
		assertNull(bean.getNinth());
	}
	
	public void testQueryDeserializationAnnotations() throws Exception {
		HttpTestRequest request = new HttpTestRequest();
		request.setMethod(HttpMethod.GET);
		request.addHeader("Content-Type", "application/x-www-form-urlencoded");
		request.addHeader("Accept", "text/xml");
		request.addParameter("first", "one");
		request.addParameter("second", "two");
		request.addParameter("third", "three");
		
		Deserializer<QueryAnnotatedBean> deserializer = DeserializerFactory.getInstance().createDeserializer(QueryAnnotatedBean.class, "application/x-www-form-urlencoded");
		assertEquals("application/x-www-form-urlencoded", deserializer.getMimeType());
		QueryAnnotatedBean bean = deserializer.deserialize(request);
		
		assertNotNull(bean);
		assertNull(bean.getFirst());
		assertNull(bean.getSecond());
		assertEquals("three", bean.getThird());
	}
	
	public void testQueryDeserializationFactory() throws Exception {
		HttpTestRequest request = new HttpTestRequest();
		request.setMethod(HttpMethod.GET);
		request.addHeader("Content-Type", "application/x-www-form-urlencoded");
		request.addHeader("Accept", "text/xml");
		request.addParameter("first", "true");
		request.addParameter("second", "0.567");
		request.addParameter("third", "0.452");
		request.addParameter("fourth", "10");
		request.addParameter("fifth", "3400000000");
		request.addParameter("sixth", "32767");
		request.addParameter("seventh", "String");
		request.addParameter("eight", "2011-08-24T08:50:23+02:00");
		request.addParameter("ninth", "2011-08-24T08:52:23+02:00");
		
		Deserializer<InBean> deserializer = DeserializerFactory.getInstance().createDeserializer(InBean.class, "application/x-www-form-urlencoded");
		Deserializer<InBean> deserializer2 = DeserializerFactory.getInstance().createDeserializer(InBean.class, "application/x-www-form-urlencoded");
		assertTrue(deserializer == deserializer2);
		
		deserializer = DeserializerFactory.getInstance().createDeserializer(InBean.class, "nonexistent/contentType");
		assertNull(deserializer);
		
		try {
			DeserializerFactory.getInstance().createDeserializer(NotAnnotatedBean.class, "application/x-www-form-urlencoded");
			fail("Must not be possible to create deserializer for bean not annotated as deserializable");
		} catch(SerializerFactoryException e) {}

		try {
			DeserializerFactory.getInstance().createDeserializer(NotDeserializableBean.class, "application/x-www-form-urlencoded");
			fail("Must not be possible to create deserializer for bean where deserialize = false");
		} catch(SerializerFactoryException e) {}

		try {
			DeserializerFactory.getInstance().createDeserializer(NotQueryBean.class, "application/x-www-form-urlencoded");
			fail("Must not be possible to create deserializer for bean where query is missing in type annotation");
		} catch(SerializerFactoryException e) {}
	}
	
	private String formatDate(Date value) {
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ssZ");
		String formatted = format.format(value);
		formatted = formatted.substring(0, formatted.length() - 2) + ":" + formatted.substring(formatted.length() - 2);
		return formatted;
	}
	
	public static Test suite() {
		return new TestSuite(QueryDeserializerTestCase.class);
	}

	public static void main(String[] argv) {
		junit.textui.TestRunner.run(suite());
	}
}
