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

package com.nginious.http.serialize;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.nginious.http.HttpRequest;

/**
 * Base class for all deserializers that deserialize beans from JSON format. Used as base class
 * by {@link JsonDeserializerCreator} when creating deserializers runtime.
 * 
 * @author Bojan Pisler, NetDigital Sweden AB
 * @param <E> the type of bean that is deserialized by this deserializer
 */
public abstract class JsonDeserializer<E> implements Deserializer<E> {
	
	private static SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
	
	private String name;
	
	/**
	 * Constructs a new JSON deserializer
	 */
	public JsonDeserializer() {
		super();
		ParameterizedType type = (ParameterizedType)getClass().getGenericSuperclass();
		@SuppressWarnings("unchecked")
		Class<E> clazz = (Class<E>)type.getActualTypeArguments()[0];
		this.name = clazz.getSimpleName();
		this.name = name.substring(0, 1).toLowerCase() + name.substring(1);
	}
	
	/**
	 * Returns the application/json mime type for this deserializer.
	 * 
	 * @return the mime type for this deserializer
	 */
	public String getMimeType() {
		return "application/json";
	}
	
	/**
	 * Deserializes a bean from the JSON body content in the specified HTTP request.
	 * 
	 * @param request the HTTP request
	 * @return the deserialized bean
	 * @throws SerializerException if unable to deserialize bean
	 */
	public E deserialize(HttpRequest request) throws SerializerException {
		if(request.getContentLength() == 0) {
			return null;
		}
		
		try {
			BufferedReader reader = request.getReader();
			StringBuffer jsonText = new StringBuffer();
			char[] buf = new char[1024];
			int len = 0;
			
			while((len = reader.read(buf)) > 0) {
				jsonText.append(buf, 0, len);
			}
			
			JSONObject object = new JSONObject(jsonText.toString());
			
			if(!object.has(this.name)) {
				throw new SerializerException("Can't find object " + this.name + " in JSON data");
			}
			
			object = object.getJSONObject(this.name);
			return deserialize(object);
		} catch(JSONException e) {
			throw new SerializerException("Can't deserialize object", e);
		} catch(IOException e) {
			throw new SerializerException("Can't deserialize object", e);
		}
	}
	
	/**
	 * Deserializes a bean from the JSON body content in the specified message.
	 * 
	 * @param message the message
	 * @return the deserialized bean
	 * @throws SerializerException if unable to deserialize bean
	 */
	public E deserialize(String message) throws SerializerException {
		if(message.length() == 0) {
			return null;
		}
		
		try {
			JSONObject object = new JSONObject(message);
			
			if(!object.has(this.name)) {
				throw new SerializerException("Can't find object " + this.name + " in JSON data");
			}
			
			object = object.getJSONObject(this.name);
			return deserialize(object);
		} catch(JSONException e) {
			throw new SerializerException("Can't deserialize object", e);
		}
	}
	
	/**
	 * Deserializes the specified JSON object.
	 * 
	 * @param object the given JSON object to deserialize
	 * @return the deserialized bean
	 * @throws SerializerException if unable to deserialize bean
	 */
	protected abstract E deserialize(JSONObject object) throws SerializerException;
	
	/**
	 * Deserializes property with the specified name in the specified json object into a boolean array.
	 * 
	 * @param object the json object
	 * @param name the property name
	 * @return the deserialized array or <code>null</code> if property doesn't exist
	 * @throws SerializerException if unable to deserialize value
	 */
	protected boolean[] deserializeBooleanArray(JSONObject object, String name) throws SerializerException {
		try {
			if(object.has(name)) {
				JSONArray array = object.getJSONArray(name);
				boolean[] outArray = new boolean[array.length()];
				
				for(int i = 0; i < array.length(); i++) {
					outArray[i] = array.getBoolean(i);
				}
				
				return outArray;
			}
			
			return null;
		} catch(JSONException e) {
			throw new SerializerException("Can't deserialize boolean array property " + name, e);			
		}
	}
	
	/**
	 * Deserializes property with the specified name from the specified json object into a boolean value.
	 * 
	 * @param object the given json object
	 * @param name the given property name
	 * @return the deserialized boolean value or <code>false</code> if property doesn't exist
	 * @throws SerializerException if unable to deserialize boolean property
	 */
	protected boolean deserializeBoolean(JSONObject object, String name) throws SerializerException {
		if(!object.has(name)) {
			return false;
		}
		
		try {			
			return object.getBoolean(name);
		} catch(JSONException e) {
			throw new SerializerException("Can't deserialize boolean property " + name, e);
		}
	}
	
	/**
	 * Deserializes property with the specified name from the specified json object into a calendar object.
	 * The serialized date must be in 'yyyy-MM-dd'T'HH:mm:ssZ' format.
	 * 
	 * @param object the given json object
	 * @param name the given property name
	 * @return the deserialized calendar or <code>null</code> if property doesn't exist
	 * @throws SerializerException if unable to deserialize calendar
	 */
	protected Calendar deserializeCalendar(JSONObject object, String name) throws SerializerException {
		if(!object.has(name)) {
			return null;
		}
		
		try {
			String value = object.getString(name);
			
			if(value == null) {
				return null;
			}
			
			if(value.matches(".*[+-][0-9]{2}:[0-9]{2}$")) {
				int lastIndex = value.lastIndexOf(':');
				value = value.substring(0, lastIndex) + value.substring(lastIndex + 1);
			}
			
			Date date = parseDate(value);
			String tz = value.substring(value.length() - 5, value.length() - 2);
			TimeZone zone = TimeZone.getTimeZone("GMT" + tz);
			Calendar cal = Calendar.getInstance(zone);
			cal.setTime(date);
			return cal;
		} catch(JSONException e) {
			throw new SerializerException("Can't deserialize calendar property " + name, e);
		}
	}
	
	/**
	 * Deserializes property with the specified name from the specified json object into a data object.
	 * The serialized date must be in 'yyyy-MM-dd'T'HH:mm:ssZ' format.
	 * 
	 * @param object the given json object
	 * @param name the given property name
	 * @return the deserialized data or <code>null</code> if property doesn't exist
	 * @throws SerializerException if unable to deserialize date
	 */
	protected Date deserializeDate(JSONObject object, String name) throws SerializerException {
		if(!object.has(name)) {
			return null;
		}
		
		try {
			String value = object.getString(name);
			
			if(value.matches(".*[+-][0-9]{2}:[0-9]{2}$")) {
				int lastIndex = value.lastIndexOf(':');
				value = value.substring(0, lastIndex) + value.substring(lastIndex + 1);
			}
			
			return parseDate(value);
		} catch(JSONException e) {
			throw new SerializerException("Can't deserialize date property " + name, e);
		}		
	}
	
	/**
	 * Parses the specified value into a date. The provided value must have the format
	 * 'yyyy-MM-dd'T'HH:mm:ssZ'.
	 * 
	 * @param value the provided date string value
	 * @return the created date
	 * @throws SerializerException if unable to parse value into a date
	 */
	private Date parseDate(String value) throws SerializerException {
		try {
			return format.parse(value);
		} catch(ParseException e) {
			throw new SerializerException("Can't parse date property " + value, e);
		}
	}
	
	/**
	 * Deserializes property with the specified name in the specified json object into a double array.
	 * 
	 * @param object the json object
	 * @param name the property name
	 * @return the deserialized array or <code>null</code> if property doesn't exist
	 * @throws SerializerException if unable to deserialize value
	 */
	protected double[] deserializeDoubleArray(JSONObject object, String name) throws SerializerException {
		try {
			if(object.has(name)) {
				JSONArray array = object.getJSONArray(name);
				double[] outArray = new double[array.length()];
				
				for(int i = 0; i < array.length(); i++) {
					outArray[i] = array.getDouble(i);
				}
				
				return outArray;
			}
			
			return null;
		} catch(JSONException e) {
			throw new SerializerException("Can't deserialize double array property " + name, e);			
		}
	}
	
	/**
	 * Deserializes property with the specified name from the specified json object into a double.
	 * 
	 * @param object the json object
	 * @param name the property name
	 * @return the deserialized double or <code>0.0d</code> if property doesn't exist
	 * @throws SerializerException if unable to deserialize value
	 */
	protected double deserializeDouble(JSONObject object, String name) throws SerializerException {
		try {
			if(object.has(name)) {
				return object.getDouble(name);
			}
			
			return 0.0d;
		} catch(JSONException e) {
			throw new SerializerException("Can't deserialize double property " + name, e);
		}
	}
	
	/**
	 * Deserializes property with the specified name in the specified json object into a float array.
	 * 
	 * @param object the json object
	 * @param name the property name
	 * @return the deserialized array or <code>null</code> if property doesn't exist
	 * @throws SerializerException if unable to deserialize value
	 */
	protected float[] deserializeFloatArray(JSONObject object, String name) throws SerializerException {
		try {
			if(object.has(name)) {
				JSONArray array = object.getJSONArray(name);
				float[] outArray = new float[array.length()];
				
				for(int i = 0; i < array.length(); i++) {
					outArray[i] = (float)array.getDouble(i);
				}
				
				return outArray;
			}
			
			return null;
		} catch(JSONException e) {
			throw new SerializerException("Can't deserialize float array property " + name, e);			
		}
	}
	
	/**
	 * Deserializes property with the specified name from the specified json object into a float.
	 * 
	 * @param object the json object
	 * @param name the property name
	 * @return the deserialized float or <code>0.0f</code> if property doesn't exist
	 * @throws SerializerException if unable to deserialize value
	 */
	protected float deserializeFloat(JSONObject object, String name) throws SerializerException {
		try {
			if(object.has(name)) {
				return (float)object.getDouble(name);				
			}
			
			return 0.0f;
		} catch(JSONException e) {
			throw new SerializerException("Can't deserialize float property " + name, e);
		}
	}
	
	/**
	 * Deserializes property with the specified name in the specified json object into a integer array.
	 * 
	 * @param object the json object
	 * @param name the property name
	 * @return the deserialized array or <code>null</code> if property doesn't exist
	 * @throws SerializerException if unable to deserialize value
	 */
	protected int[] deserializeIntArray(JSONObject object, String name) throws SerializerException {
		try {
			if(object.has(name)) {
				JSONArray array = object.getJSONArray(name);
				int[] outArray = new int[array.length()];
				
				for(int i = 0; i < array.length(); i++) {
					outArray[i] = array.getInt(i);
				}
				
				return outArray;
			}
			
			return null;
		} catch(JSONException e) {
			throw new SerializerException("Can't deserialize integer array property " + name, e);			
		}
	}
	
	/**
	 * Deserializes property with the specified name from the specified json object into a integer.
	 * 
	 * @param object the json object to deserialize property from
	 * @param name name of the property
	 * @return the deserialized integer or <code>0</code> if property doesn't exist
	 * @throws SerializerException uf unable to deserialize value
	 */
	protected int deserializeInt(JSONObject object, String name) throws SerializerException {
		try {
			if(object.has(name)) {
				return object.getInt(name);				
			}
			
			return 0;
		} catch(JSONException e) {
			throw new SerializerException("Can't deserialize int property " + name, e);
		}
	}
	
	/**
	 * Deserializes property with the specified name in the specified json object into a long array.
	 * 
	 * @param object the json object
	 * @param name the property name
	 * @return the deserialized array or <code>null</code> if property doesn't exist
	 * @throws SerializerException if unable to deserialize value
	 */
	protected long[] deserializeLongArray(JSONObject object, String name) throws SerializerException {
		try {
			if(object.has(name)) {
				JSONArray array = object.getJSONArray(name);
				long[] outArray = new long[array.length()];
				
				for(int i = 0; i < array.length(); i++) {
					outArray[i] = array.getLong(i);
				}
				
				return outArray;
			}
			
			return null;
		} catch(JSONException e) {
			throw new SerializerException("Can't deserialize long array property " + name, e);			
		}
	}
	
	/**
	 * Deserializes property with the specified name in the specified json object into a long.
	 * 
	 * @param object the json object
	 * @param name the property name
	 * @return the deserialized value or <code>0</code> if property doesn't exist
	 * @throws SerializerException if unable to deserialize value
	 */
	protected long deserializeLong(JSONObject object, String name) throws SerializerException {
		try {
			if(object.has(name)) {
				return object.getLong(name);
			}
			
			return 0;
		} catch(JSONException e) {
			throw new SerializerException("Can't deserialize long property " + name, e);
		}
	}
	
	/**
	 * Deserializes property with the specified name in the specified json object into a short array.
	 * 
	 * @param object the json object
	 * @param name the property name
	 * @return the deserialized array or <code>null</code> if property doesn't exist
	 * @throws SerializerException if unable to deserialize value
	 */
	protected short[] deserializeShortArray(JSONObject object, String name) throws SerializerException {
		try {
			if(object.has(name)) {
				JSONArray array = object.getJSONArray(name);
				short[] outArray = new short[array.length()];
				
				for(int i = 0; i < array.length(); i++) {
					outArray[i] = (short)array.getInt(i);
				}
				
				return outArray;
			}
			
			return null;
		} catch(JSONException e) {
			throw new SerializerException("Can't deserialize short array property " + name, e);			
		}
	}
	
	/**
	 * Deserializes property with the specified name in the specified json object into a short.
	 * 
	 * @param object the json object
	 * @param name the property name
	 * @return the deserialized value or <code>0</code> if property doesn't exist
	 * @throws SerializerException if unable to deserialize value
	 */
	protected short deserializeShort(JSONObject object, String name) throws SerializerException {
		try {
			if(object.has(name)) {
				return (short)object.getInt(name);
			}
			
			return 0;
		} catch(JSONException e) {
			throw new SerializerException("Can't deserialize short property " + name, e);
		}
	}
	
	/**
	 * Deserializes property with the specified name in the specified json object into a string array.
	 * 
	 * @param object the json object
	 * @param name the property name
	 * @return the deserialized array or <code>null</code> if property doesn't exist
	 * @throws SerializerException if unable to deserialize value
	 */
	protected String[] deserializeStringArray(JSONObject object, String name) throws SerializerException {
		try {
			if(object.has(name)) {
				JSONArray array = object.getJSONArray(name);
				String[] outArray = new String[array.length()];
				
				for(int i = 0; i < array.length(); i++) {
					outArray[i] = array.getString(i);
				}
				
				return outArray;
			}
			
			return null;
		} catch(JSONException e) {
			throw new SerializerException("Can't deserialize string array property " + name, e);			
		}
	}
	
	/**
	 * Deserializes property with the specified name in the specified json object into a string.
	 * 
	 * @param object the json object
	 * @param name the property name
	 * @return the deserialized value or <code>null</code> if property doesn't exist
	 * @throws SerializerException if unable to deserialize value
	 */
	protected String deserializeString(JSONObject object, String name) throws SerializerException {
		try {
			if(object.has(name)) {
				return object.getString(name);
			}
			
			return null;
		} catch(JSONException e) {
			throw new SerializerException("Can't deserialize string property " + name, e);
		}
	}
}
