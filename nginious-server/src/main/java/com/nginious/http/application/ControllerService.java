package com.nginious.http.application;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.HashSet;

import com.nginious.http.HttpException;
import com.nginious.http.HttpMethod;
import com.nginious.http.HttpRequest;
import com.nginious.http.HttpResponse;
import com.nginious.http.HttpStatus;
import com.nginious.http.serialize.Deserializer;
import com.nginious.http.serialize.DeserializerFactory;
import com.nginious.http.serialize.DeserializerFactoryImpl;
import com.nginious.http.serialize.Serializer;
import com.nginious.http.serialize.SerializerException;
import com.nginious.http.serialize.SerializerFactory;
import com.nginious.http.serialize.SerializerFactoryException;
import com.nginious.http.serialize.SerializerFactoryImpl;
import com.nginious.http.websocket.StatusCode;
import com.nginious.http.websocket.WebSocketBinaryMessage;
import com.nginious.http.websocket.WebSocketException;
import com.nginious.http.websocket.WebSocketSession;
import com.nginious.http.websocket.WebSocketTextMessage;

/**
 * A controller service manages invocations of the appropriate controller methods for HTTP requests. A subclass of this class 
 * is created runtime for each loaded controller. The subclasses are created using the {@link ControllerServiceFactory} which inspects
 * the controller class and its methods and generates bytecode which override one or more of the following methods to call the
 * appropriate methods in the controller class.
 * 
 * <ul>
 *   <li>{@link #executeGet(HttpRequest, HttpResponse)} - called to execute a HTTP GET.</li>
 *   <li>{@link #executePost(HttpRequest, HttpResponse)} - called to execute a HTTP POST.</li>
 *   <li>{@link #executePut(HttpRequest, HttpResponse)} - called to execute a HTTP PUT.</li>
 *   <li>{@link #executeDelete(HttpRequest, HttpResponse)} - called to execute a HTTP DELETE.</li>
 *   <li>{@link #executeOpen(HttpRequest, HttpResponse, WebSocketSession)} - executed when a new web socket session is opened.</li>
 *   <li>{@link #executeTextMessage(WebSocketTextMessage, WebSocketSession)} - executed when a web socket text message is received.</li>
 *   <li>{@link #executeBinaryMessage(WebSocketBinaryMessage, WebSocketSession)} - executed when a binary web socket message is received.</li>
 *   <li>{@link #executeClose(WebSocketSession)} - executed when a web socket session is closed.</li>
 * </ul>
 *
 * @see com.nginious.http.annotation.Controller 
 * @see com.nginious.http.annotation.Request 
 * @see com.nginious.http.annotation.Method 
 * @see com.nginious.http.application.ControllerServiceFactory 
 * @author Bojan Pisler, NetDigital Sweden AB
 *
 */
public abstract class ControllerService extends HttpService {
	
	private Object controller;
	
	private String httpMethods;
	
	private Application application;
	
	private ApplicationClassLoader classLoader;
	
	private HashSet<Class<?>> clazzes;
	
	private SerializerFactoryImpl serializerFactory;
	
	private DeserializerFactoryImpl deserializerFactory;
	
	/**
	 * Constructs a new empty controller service.
	 */
	public ControllerService() {
		super();
	}
	
	/**
	 * Sets the application for this controller service.
	 * 
	 * @param application the application
	 */
	void setApplication(Application application) {
		this.application = application;
	}
	
	/**
	 * Returns the application for this controller service.
	 * 
	 * @return the application
	 */
	public Application getApplication() {
		return this.application;
	}
	
	/**
	 * Sets the controller that this controller service should invoke.
	 * 
	 * @param controller the controller to invoke
	 */
	void setController(Object controller) {
		this.controller = controller;
	}
	
	/**
	 * Returns the controller that this controller service invokes for HTTP requests.
	 * 
	 * @return the controller
	 */
	public Object getController() {
		return this.controller;
	}
	
	/**
	 * Sets the serializer factory to the specified factory for this controller service.
	 * 
	 * @param serializerFactory the serializer factory
	 */
	void setSerializerFactory(SerializerFactoryImpl serializerFactory) {
		this.serializerFactory = serializerFactory;
	}
	
	/**
	 * Sets the deserializer factory to the specified factory for this controller service.
	 * 
	 * @param deserializerFactory the deserializer factory
	 */
	void setDeserializerFactory(DeserializerFactoryImpl deserializerFactory) {
		this.deserializerFactory = deserializerFactory;
	}
	
	/**
	 * Returns the deserializer factory for this controller service.
	 * 
	 * @return the deserializer factory
	 */
	protected DeserializerFactory getDeserializerFactory() {
		return this.deserializerFactory;
	}
	
	/**
	 * Sets the application class loader that this controller services controller was loaded with.
	 *  
	 * @param classLoader the class loader
	 */
	void setClassLoader(ApplicationClassLoader classLoader) {
		this.classLoader = classLoader;
	}
	
	/**
	 * Returns the serializer factory for this controller service.
	 * 
	 * @return the serializer factory
	 */
	protected SerializerFactory getSerializerFactory() {
		return this.serializerFactory;
	}
	
	/**
	 * Sets the classes for this controller service
	 * 
	 * @param classes the classes
	 */
	void setClasses(HashSet<Class<?>> clazzes) {
		this.clazzes = clazzes;
	}
	
	/**
	 * Checks if the controller class or any class that the controller depends has been modified.
	 * 
	 * @return <code>true</code> if any class has been modified, <code>false</code> otherwise
	 */
	boolean anyClassChanged() {
		for(Class<?> clazz : this.clazzes) {
			if(classLoader.reloadIfModified(clazz)) {
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Sets the HTTP methods handled by the controller for this controller service. Used by HTTP server for
	 * HTTP OPTIONS request.
	 * 
	 * @param httpMethods the HTTP methods handled by the controller for this controller service
	 */
	void setHttpMethods(String httpMethods) {
		this.httpMethods = httpMethods;
	}
	
	/**
	 * Returns the HTTP methods supported by the controller for this controller service.
	 * 
	 * @return the HTTP methods handled by the controller for this controller service
	 */
	String getHttpMethods() {
		return this.httpMethods;
	}
	
	/**
	 * Invokes this controller service with the specified HTTP request and response. This method calls one of the
	 * {@link #executeGet(HttpRequest, HttpResponse)}, {@link #executePost(HttpRequest, HttpResponse)},
	 * {@link #executePut(HttpRequest, HttpResponse)} or {@link #executeDelete(HttpRequest, HttpResponse)} methods
	 * which may be overridden by subclasses to call the appropriate method in the controller.
	 * 
	 * @param request the HTTP request
	 * @param response the HTTP response
	 * @return a result indicating if execution is done, should continue or is asynchronous.
	 * @throws HttpException if the HTTP request is invalid or if the service is unable to process the request
	 * @throws IOException if an I/O error occurs
	 */
	public HttpServiceResult invoke(HttpRequest request, HttpResponse response) throws HttpException, IOException {
		HttpServiceResult result = HttpServiceResult.DONE;
		HttpMethod method = request.getMethod();
		response = new ControllerResponse(this, request, response);
		
		if(method.equals(HttpMethod.HEAD)) {
			result = executeGet(request, response);			
		} else if(method.equals(HttpMethod.GET)) {
			String value = request.getHeader("Upgrade");
			
			if(value != null && value.toLowerCase().equals("websocket")) {
				WebSocketSession session = (WebSocketSession)request.getAttribute("se.netdigital.http.websocket.WebSocketSession");
				result = executeOpen(request, response, session);
			} else {
				result = executeGet(request, response);
			}
		} else if(method.equals(HttpMethod.POST)) {
			result = executePost(request, response);
		} else if(method.equals(HttpMethod.PUT)) {
			result = executePut(request, response);
		} else if(method.equals(HttpMethod.DELETE)) {
			result = executeDelete(request, response);
		} else {
			response.setStatus(HttpStatus.BAD_REQUEST, "Invalid HTTP method");
		}
		
		return result;
	}
	
	/**
	 * Called to execute a HTTP GET using the specified HTTP request and response. Default behavior is to throw a
	 * {@link com.nginious.http.HttpException} with status {@link com.nginious.http.HttpStatus#METHOD_NOT_ALLOWED}.
	 * This method may be overridden by subclasses to call the appropriate method in the controller. Method is
	 * overridden if the controller contains a method annotated with {@link com.nginious.http.annotation.Request}
	 * where the {@link com.nginious.http.annotation.Request#methods()} contains the value {@link com.nginious.http.HttpMethod#GET}.
	 * 
	 * @param request the HTTP request
	 * @param response the HTTP response
	 * @return a result indicating if execution is done, should continue or is asynchronous
	 * @throws HttpException if the HTTP request is invalid or if the service is unable to process the request
	 * @throws IOException if an I/O error occurs
	 */
	public HttpServiceResult executeGet(HttpRequest request, HttpResponse response) throws HttpException, IOException {
		throw new HttpException(HttpStatus.METHOD_NOT_ALLOWED, "GET method not allowed");
	}
	
	/**
	 * Called to execute a HTTP POST using the specified HTTP request and response. Default behavior is to throw a
	 * {@link com.nginious.http.HttpException} with status {@link com.nginious.http.HttpStatus#METHOD_NOT_ALLOWED}.
	 * This method may be overridden by subclasses to call the appropriate method in the controller. Method is
	 * overridden if the controller contains a method annotated with {@link com.nginious.http.annotation.Request}
	 * where the {@link com.nginious.http.annotation.Request#methods()} contains the value {@link com.nginious.http.HttpMethod#POST}.
	 * 
	 * @param request the HTTP request
	 * @param response the HTTP response
	 * @return a result indicating if execution is done, should continue or is asynchronous
	 * @throws HttpException if the HTTP request is invalid or if the service is unable to process the request
	 * @throws IOException if an I/O error occurs
	 */
	public HttpServiceResult executePost(HttpRequest request, HttpResponse response) throws HttpException, IOException {
		throw new HttpException(HttpStatus.METHOD_NOT_ALLOWED, "POST method not allowed");
	}
	
	/**
	 * Called to execute a HTTP PUT using the specified HTTP request and response. Default behavior is to throw a
	 * {@link com.nginious.http.HttpException} with status {@link com.nginious.http.HttpStatus#METHOD_NOT_ALLOWED}.
	 * This method may be overridden by subclasses to call the appropriate method in the controller. Method is
	 * overridden if the controller contains a method annotated with {@link com.nginious.http.annotation.Request}
	 * where the {@link com.nginious.http.annotation.Request#methods()} contains the value {@link com.nginious.http.HttpMethod#PUT}.
	 * 
	 * @param request the HTTP request
	 * @param response the HTTP response
	 * @return a result indicating if execution is done, should continue or is asynchronous
	 * @throws HttpException if the HTTP request is invalid or if the service is unable to process the request
	 * @throws IOException if an I/O error occurs
	 */
	public HttpServiceResult executePut(HttpRequest request, HttpResponse response) throws HttpException, IOException {
		throw new HttpException(HttpStatus.METHOD_NOT_ALLOWED, "PUT method not allowed");
	}
	
	/**
	 * Called to execute a HTTP DELETE using the specified HTTP request and response. Default behavior is to throw a
	 * {@link com.nginious.http.HttpException} with status {@link com.nginious.http.HttpStatus#METHOD_NOT_ALLOWED}.
	 * This method may be overridden by subclasses to call the appropriate method in the controller. Method is
	 * overridden if the controller contains a method annotated with {@link com.nginious.http.annotation.Request}
	 * where the {@link com.nginious.http.annotation.Request#methods()} contains the value {@link com.nginious.http.HttpMethod#DELETE}.
	 * 
	 * @param request the HTTP request
	 * @param response the HTTP response
	 * @return a result indicating if execution is done, should continue or is asynchronous
	 * @throws HttpException if the HTTP request is invalid or if the service is unable to process the request
	 * @throws IOException if an I/O error occurs
	 */
	public HttpServiceResult executeDelete(HttpRequest request, HttpResponse response) throws HttpException, IOException {
		throw new HttpException(HttpStatus.METHOD_NOT_ALLOWED, "DELETE method not allowed");
	}
	
	/**
	 * Executed when a new web socket session is opened. Default behavior is to throw a {@link com.nginious.http.HttpException}
	 * with status {@link com.nginious.http.HttpStatus#METHOD_NOT_ALLOWED}. This method may be overridden by subclasses to call the
	 * appropriate method in the controller. The controller method must be annotated with a {@link com.nginious.http.annotation.Message}
	 * annotation where the {@link com.nginious.http.annotation.Message#operations()} attribute contains the value
	 * {@link com.nginious.http.websocket.WebSocketOperation#OPEN}.
	 *  
	 * @param request the HTTP request
	 * @param response the HTTP response
	 * @param session the opened web socket session
	 * @throws HttpException if the HTTP request is invalid or if the service unable to process the request
	 * @throws IOException if an I/O error occurs
	 */
	public HttpServiceResult executeOpen(HttpRequest request, HttpResponse response, WebSocketSession session) throws HttpException, IOException {
		throw new HttpException(HttpStatus.METHOD_NOT_ALLOWED, "Open websocket method not allowed");
	}
	
	/**
	 * Executed when a binary message arrives. Default behavior is to throw a {@link com.nginious.http.websocket.WebSocketException}
	 * with status code {@link com.nginious.http.websocket.StatusCode#UNSUPPORTED_DATA}. This method may be overridden by subclasses to call the
	 * appropriate method in the controller. The controller method must be annotated with a {@link com.nginious.http.annotation.Message}
	 * annotation where the {@link com.nginious.http.annotation.Message#operations()} attribute contains the value
	 * {@link com.nginious.http.websocket.WebSocketOperation#BINARY}.
	 *
	 * @param message the received binary message
	 * @param session the opened web socket session
	 * @throws WebSocketException if the message is invalid or if the service is unable to process the request
	 * @throws IOException if an I/O error occurs
	 */
	public void executeBinaryMessage(WebSocketBinaryMessage message, WebSocketSession session) throws WebSocketException, IOException {
		throw new WebSocketException(StatusCode.UNSUPPORTED_DATA, "Binary data not allowed");
	}
	
	/**
	 * Executed when a text message arrives. Default behavior is to throw a {@link com.nginious.http.websocket.WebSocketException}
	 * with status code {@link com.nginious.http.websocket.StatusCode#UNSUPPORTED_DATA}. This method may be overridden by subclasses to call the
	 * appropriate method in the controller. The controller method must be annotated with a {@link com.nginious.http.annotation.Message}
	 * annotation where the {@link com.nginious.http.annotation.Message#operations()} attribute contains the value
	 * {@link com.nginious.http.websocket.WebSocketOperation#TEXT}.
	 *
	 * @param message the received text message
	 * @param session the opened web socket session
	 * @throws WebSocketException if the message is invalid or if the service is unable to process the request
	 * @throws IOException if an I/O error occurs
	 */
	public void executeTextMessage(WebSocketTextMessage message, WebSocketSession session) throws WebSocketException, IOException {
		throw new WebSocketException(StatusCode.UNSUPPORTED_DATA, "Text data not allowed");
	}
	
	/**
	 * Executed when a web socket session is closed Default behavior is to do nothing. This method may be overridden by subclasses to call the
	 * appropriate method in the controller. The controller method must be annotated with a {@link com.nginious.http.annotation.Message}
	 * annotation where the {@link com.nginious.http.annotation.Message#operations()} attribute contains the value
	 * {@link com.nginious.http.websocket.WebSocketOperation#CLOSE}.
	 *  
	 * @param session the closed web socket session
	 * @throws WebSocketException if the web socket session is invalid
	 * @throws IOException if an I/O error occurs
	 */
	public void executeClose(WebSocketSession session) throws WebSocketException, IOException {
		return;
	}
	
	/**
	 * Called by subclasses to handled text based responses from controllers. Controllers may return responses as return values
	 * from the invoked method. The response is handled with the following rules.
	 * 
	 * <ul>
	 *   <li>If the response content ends with '.xsp' it is assumed to be a dispatch to a xsp page.</li>
	 *   <li>If the response contents text data the data is written to the response body with content type 'text/plain'.</li>
	 * </ul>
	 * 
	 * @param content the response content
	 * @param request the HTTP request
	 * @param response the HTTP response
	 * @throws IOException if an I/O error occurs
	 * @throws HttpException if a HTTP exception occurs while processing the response data
	 */
	protected void response(String content, HttpRequest request, HttpResponse response) throws IOException, HttpException {
		if(content == null) {
			response.setStatus(HttpStatus.NO_CONTENT);
		} else if(content.endsWith(".xsp")) {
			request.dispatch(content);
		} else {
			response.setContentType("text/plain");
			response.setCharacterEncoding("utf-8");
			response.setContentLength(content.getBytes().length);
			PrintWriter writer = response.getWriter();
			writer.print(content);
		}
	}
	
	/**
	 * Called by subclasses to handled bean collection based responses from controllers. Controllers may return responses as 
	 * return values from the invoked method. An appropriate serializer is created or selected based on the following rules.
	 * 
	 * <ul>
	 *   <li>The specified content type.</li>
	 *   <li>The {@link com.nginious.http.annotation.Serializable} annotation of the bean is inspected.</li>
	 * </ul>
	 * 
	 * See {@link com.nginious.http.serialize.SerializerFactoryImpl} for further details on the serialization mechanism.
	 *
	 * @param bean the bean collection to serialize
	 * @param beanClassName name of bean class
	 * @param contentType the content type
	 * @throws WebSocketException if unable to serialize bean
	 */
	protected <T> String serialize(Collection<T> items, String beanClassName, String contentType) throws WebSocketException {
		try {
			if(items != null) {
				@SuppressWarnings("unchecked")
				Class<T> beanClazz = (Class<T>)classLoader.loadClass(beanClassName);
				Serializer<T> serializer = serializerFactory.createSerializer(beanClazz, contentType);
				
				if(serializer == null) {
					throw new WebSocketException(StatusCode.INTERNAL_SERVER_ERROR, "Content type not supported '" + contentType + "'");
				}
				
				StringWriter outWriter = new StringWriter();
				PrintWriter writer = new PrintWriter(outWriter);
				serializer.serialize(writer, items);
				writer.flush();
				return outWriter.toString();
			} else {
				return null;
			}
		} catch(ClassNotFoundException e) {
			throw new WebSocketException(StatusCode.INTERNAL_SERVER_ERROR, "Serialization failed", e);			
		} catch(SerializerException e) {
			throw new WebSocketException(StatusCode.INTERNAL_SERVER_ERROR, "Serialization failed: " + e.getMessage(), e);			 
		} catch(SerializerFactoryException e) {
			throw new WebSocketException(StatusCode.INTERNAL_SERVER_ERROR, "Serialization failed: " + e.getMessage(), e);			
		}
	}
	
	/**
	 * Called by subclasses to handled bean collection based responses from controllers. Controllers may return responses as 
	 * return values from the invoked method. An appropriate serializer is created or selected based on the following rules.
	 * 
	 * <ul>
	 *   <li>The accept header in the HTTP request is inspected to determine the preferred format of the client.</li>
	 *   <li>The {@link com.nginious.http.annotation.Serializable} annotation of the bean is inspected.</li>
	 * </ul>
	 * 
	 * See {@link com.nginious.http.serialize.SerializerFactoryImpl} for further details on the serialization mechanism.
	 *
	 * @param bean the bean collection to serialize
	 * @param beanClassName name of bean class
	 * @param request the HTTP request
	 * @param response the HTTP response
	 * @throws IOException if an I/O error occurs
	 * @throws HttpException if a HTTP exception occurs while processing the response data
	 */
	protected <T> void serialize(Collection<T> items, String beanClassName, HttpRequest request, HttpResponse response) throws HttpException, IOException {
		try {
			if(items != null) {
				@SuppressWarnings("unchecked")
				Class<T> beanClazz = (Class<T>)classLoader.loadClass(beanClassName);
				String acceptHeader = request.getHeader("Accept");
				Serializer<T> serializer = serializerFactory.createSerializer(beanClazz, acceptHeader);
				
				if(serializer == null) {
					throw new HttpException(HttpStatus.BAD_REQUEST, "No acceptable content type in '" + acceptHeader + "'");
				}
				
				response.setContentType(serializer.getMimeType());
				response.setCharacterEncoding("utf-8");
				
				ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
				OutputStreamWriter outWriter = new OutputStreamWriter(byteOut, "utf-8");
				PrintWriter writer = new PrintWriter(outWriter);
				serializer.serialize(writer, items);
				writer.flush();
				
				byte[] data = byteOut.toByteArray();
				response.setContentLength(data.length);
				OutputStream out = response.getOutputStream();
				out.write(data);				
			} else {
				serializeVoid(response);
			}
		} catch(ClassNotFoundException e) {
			throw new HttpException(HttpStatus.INTERNAL_SERVER_ERROR, "Serialization failed", e);			
		} catch(UnsupportedEncodingException e) {
			throw new HttpException(HttpStatus.INTERNAL_SERVER_ERROR, "Serialization failed", e);
		} catch(SerializerException e) {
			throw new HttpException(HttpStatus.BAD_REQUEST, "Serialization failed: " + e.getMessage(), e);
		} catch(SerializerFactoryException e) {
			throw new HttpException(HttpStatus.INTERNAL_SERVER_ERROR, "Serialization failed: " + e.getMessage(), e);
		}		
	}
	
	/**
	 * Called by subclasses to handle bean based responses from controllers. Controllers may returns responses as returns values
	 * from the invoked method. An appropriate serializer is created or selected based on the following rules.
	 * 
	 * <ul>
	 *   <li>The specified content type is inspected to determine the preferred format.</li>
	 *   <li>The {@link com.nginious.http.annotation.Serializable} annotation of the bean is inspected.</li>
	 * </ul>
	 * 
	 * See {@link com.nginious.http.serialize.SerializerFactoryImpl} for further details on the serialization mechanism.
	 *
	 * @param bean the bean to serialize
	 * @param contentType the content type
	 * @throws WebSocketException if unable to serialize response data
	 */
	protected <T> String serialize(T bean, String contentType) throws WebSocketException {
		try {
			if(bean != null) {
				@SuppressWarnings("unchecked")
				Class<T> beanClazz = (Class<T>)bean.getClass();
				Serializer<T> serializer = serializerFactory.createSerializer(beanClazz, contentType);
				
				if(serializer == null) {
					throw new WebSocketException(StatusCode.INTERNAL_SERVER_ERROR, "Content type not supported '" + contentType + "'");
				}
				
				StringWriter outWriter = new StringWriter();
				PrintWriter writer = new PrintWriter(outWriter);
				serializer.serialize(writer, bean);
				writer.flush();
				return outWriter.toString();
			} else {
				return null;
			}
		} catch(SerializerException e) {
			throw new WebSocketException(StatusCode.INTERNAL_SERVER_ERROR, "Serialization failed: " + e.getMessage(), e);
		} catch(SerializerFactoryException e) {
			throw new WebSocketException(StatusCode.INTERNAL_SERVER_ERROR, "Serialization failed: " + e.getMessage(), e);
		}		
	}
	
	/**
	 * Called by subclasses to handled bean based responses from controllers. Controllers may return responses as return values
	 * from the invoked method. An appropriate serializer is created or selected based on the following rules.
	 * 
	 * <ul>
	 *   <li>The accept header in the HTTP request is inspected to determine the preferred format of the client.</li>
	 *   <li>The {@link com.nginious.http.annotation.Serializable} annotation of the bean is inspected.</li>
	 * </ul>
	 * 
	 * See {@link com.nginious.http.serialize.SerializerFactoryImpl} for further details on the serialization mechanism.
	 *
	 * @param bean the bean to serialize
	 * @param request the HTTP request
	 * @param response the HTTP response
	 * @throws IOException if an I/O error occurs
	 * @throws HttpException if a HTTP exception occurs while processing the response data
	 */
	protected <T> void serialize(T bean, HttpRequest request, HttpResponse response) throws HttpException, IOException {
		try {
			if(bean != null) {
				@SuppressWarnings("unchecked")
				Class<T> beanClazz = (Class<T>)bean.getClass();
				String acceptHeader = request.getHeader("Accept");
				Serializer<T> serializer = serializerFactory.createSerializer(beanClazz, acceptHeader);
				
				if(serializer == null) {
					throw new HttpException(HttpStatus.BAD_REQUEST, "No acceptable content type in '" + acceptHeader + "'");
				}
				
				response.setContentType(serializer.getMimeType());
				response.setCharacterEncoding("utf-8");
				
				ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
				OutputStreamWriter outWriter = new OutputStreamWriter(byteOut, "utf-8");
				PrintWriter writer = new PrintWriter(outWriter);
				serializer.serialize(writer, bean);
				writer.flush();
				
				byte[] data = byteOut.toByteArray();
				response.setContentLength(data.length);
				OutputStream out = response.getOutputStream();
				out.write(data);
			} else {
				serializeVoid(response);
			}
		} catch(UnsupportedEncodingException e) {
			throw new HttpException(HttpStatus.INTERNAL_SERVER_ERROR, "Serialization failed", e);
		} catch(SerializerException e) {
			throw new HttpException(HttpStatus.BAD_REQUEST, "Serialization failed: " + e.getMessage(), e);
		} catch(SerializerFactoryException e) {
			throw new HttpException(HttpStatus.INTERNAL_SERVER_ERROR, "Serialization failed: " + e.getMessage(), e);
		}		
	}
	
	/**
	 * Sets a {@link com.nginious.http.HttpStatus#NO_CONTENT} status in the specified response. The status is set
	 * if no previous response has been set by the controller.
	 * 
	 * @param response the HTTP response
	 * @throws HttpException if unable to set status
	 */
	protected void serializeVoid(HttpResponse response) throws HttpException {
		if(!response.isCommitted()) {
			response.setStatus(HttpStatus.NO_CONTENT);
		}
	}
	
	/**
	 * Deserializes data from the specified request based on the specified class type. Returns an object of the
	 * specified class type with properties filled in from data in the specified request.
	 * 
	 * @param clazz the class type
	 * @param request the HTTP request
	 * @return the deserialized object
	 * @throws HttpException if unable to deserialize object
	 */
	protected <T> T deserialize(Class<T> clazz, HttpRequest request) throws HttpException {
		try {
			String contentType = request.getContentType();
			
			if(contentType == null && request.getMethod().equals(HttpMethod.GET)) {
				contentType = "application/x-www-form-urlencoded";
			}
			
			Deserializer<T> deserializer = deserializerFactory.createDeserializer(clazz, contentType);
			
			if(deserializer == null) {
				throw new HttpException(HttpStatus.BAD_REQUEST, "Invalid content type '" + contentType + "'");				
			}
			
			T object = deserializer.deserialize(request);
			return object;
		} catch(SerializerException e) {
			throw new HttpException(HttpStatus.BAD_REQUEST, "Deserialization failed: " + e.getMessage(), e);
		} catch(SerializerFactoryException e) {
			throw new HttpException(HttpStatus.INTERNAL_SERVER_ERROR, "Deserialization failed:" + e.getMessage(), e);
		}
	}
	
	/**
	 * Deserializes data from the specified message on the specified class type. Returns an object of the
	 * specified class type with properties filled in from data in the specified message.
	 * 
	 * @param clazz the class type
	 * @param message the message
	 * @param contentType the content type of the message
	 * @return the deserialized object
	 * @throws WebSocketException if unable to deserialize object
	 */
	protected <T> T deserialize(Class<T> clazz, WebSocketTextMessage message, String contentType) throws WebSocketException {
		try {
			if(contentType == null) {
				contentType = "application/xml";
			}
			
			Deserializer<T> deserializer = deserializerFactory.createDeserializer(clazz, contentType);
			
			if(deserializer == null) {
				throw new WebSocketException(StatusCode.UNSUPPORTED_DATA, "Invalid content type '" + contentType + "'");
			}
			
			T object = deserializer.deserialize(message.getMessage());
			return object;
		} catch(SerializerException e) {
			e.printStackTrace();
			throw new WebSocketException(StatusCode.INVALID_FRAME_PAYLOAD_DATA, "Deserialization failed: " + e.getMessage(), e);
		} catch(SerializerFactoryException e) {
			e.printStackTrace();
			throw new WebSocketException(StatusCode.INTERNAL_SERVER_ERROR, "Deserialization failed: " + e.getMessage(), e);
		}
	}
	
	protected Boolean decodeObjectBooleanType(String value) {
		return new Boolean(value);
	}
	
	protected boolean decodePrimitiveBooleanType(String value) {
		return Boolean.parseBoolean(value);
	}
	
	protected Byte decodeObjectByteType(String value) {
		try {
			return new Byte(value);
		} catch(NumberFormatException e) {
			return null;
		}
	}
	
	protected byte decodePrimitiveByteType(String value) {
		try {
			return Byte.parseByte(value);
		} catch(NumberFormatException e) {
			return 0;
		}
	}
	
	protected Short decodeObjectShortType(String value) {
		try {
			return new Short(value);
		} catch(NumberFormatException e) {
			return null;
		}
	}
	
	protected short decodePrimitiveShortType(String value) {
		try {
			return Short.parseShort(value);
		} catch(NumberFormatException e) {
			return 0;
		}
	}
	
	protected Integer decodeObjectIntType(String value) {
		try {
			return new Integer(value);
		} catch(NumberFormatException e) {
			return null;
		}
	}
	
	protected int decodePrimitiveIntType(String value) {
		try {
			return Integer.parseInt(value);
		} catch(NumberFormatException e) {
			return 0;
		}
	}
	
	protected Long decodeObjectLongType(String value) {
		try {
			return new Long(value);
		} catch(NumberFormatException e) {
			return null;
		}
	}
	
	protected long decodePrimitiveLongType(String value) {
		try {
			return Long.parseLong(value);
		} catch(NumberFormatException e) {
			return 0L;
		}
	}
	
	protected Float decodeObjectFloatType(String value) {
		try {
			return new Float(value);
		} catch(NumberFormatException e) {
			return null;
		}
	}
	
	protected float decodePrimitiveFloatType(String value) {
		try {
			return Float.parseFloat(value);
		} catch(NumberFormatException e) {
			return 0.0f;
		}
	}
	
	protected Double decodeObjectDoubleType(String value) {
		try {
			return new Double(value);
		} catch(NumberFormatException e) {
			return null;
		}
	}
	
	protected double decodePrimitiveDoubleType(String value) {
		try {
			return Double.parseDouble(value);
		} catch(NumberFormatException e) {
			return 0.0d;
		}
	}
	
	protected String decodeObjectStringType(String value) {
		return value;
	}
	
	protected void sendTextMessage(String data, WebSocketSession session) throws WebSocketException, IOException {
		if(data == null) {
			return;
		}
		
		session.sendTextData(data);
	}
	
	protected void sendBinaryMessage(byte[] data, WebSocketSession session) throws WebSocketException, IOException {
		if(data == null) {
			return;
		}
		
		session.sendBinaryData(data);
	}
}
