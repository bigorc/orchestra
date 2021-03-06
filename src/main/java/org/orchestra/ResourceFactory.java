package org.orchestra;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.WordUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.orchestra.builder.Builder;
import org.orchestra.json.InvalidJsonException;
import org.orchestra.json.Json;
import org.orchestra.resource.Resource;

public class ResourceFactory {
	private static String builder_base = "org.orchestra.builder.";
	private static String generic_class = "GeneralResource";
	private static String resource_base = "org.orchestra.resource.";
	
	public static Resource jsonToResource(Json json) {
		Resource resource = null;
		String builderName;
		String type = WordUtils.capitalize((String) json.get("type")); 
		if(type == null) {
			type = generic_class;
		}
		builderName = builder_base + type + "Builder";
		
		Class builderClass;
		try {
			//First, test if the type has its own builder class
			builderClass = Class.forName(builderName);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			builderClass = null;
		}
		Builder builder = null;
		if(builderClass != null) {
			try {
				builder = (Builder) builderClass.newInstance();
			} catch (InstantiationException e) {
				e.printStackTrace();
				builder = null;
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
			if(builder != null) resource = builder.makeResource(json);
		}
		
		
		if(resource != null) return resource;
		
		//Second, try a common approach
		Class resourceClass;
		try {
			resourceClass = Class.forName(resource_base + type);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			resourceClass = null;
		}
		if(resourceClass == null) return null;
		try {
			resource = (Resource) resourceClass.newInstance();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		
		for(Object key : json.keySet()) {
			if(key.equals("name") || key.equals("state") || key.equals("type")) continue;
			String methodName = "set" + WordUtils.capitalize(key.toString());
			Class parameterType = null;
			Object arg = null;
			Object value = json.get((String)key);
			if(value instanceof String) {
				parameterType = String.class;
				arg = value;
			} else if (value instanceof JSONObject) {
				parameterType = Resource.class;
				arg = makeResource((JSONObject) value);
			} else if (value instanceof JSONArray) {
				parameterType = List.class;
				arg = makeResources((JSONArray) value);
			}
			Method method;
			try {
				method = resourceClass.getMethod(methodName, parameterType);
				method.invoke(resource, arg);
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
				method = null;
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
		}
		resource.setJson(json);
		return resource;
	}
	
	public static Resource makeResource(JSONObject obj) {
		Json json = new Json(obj);
		return jsonToResource(json);
	}

	private static Resource makeResource(JSONObject obj, Json outerJson) {
		Json json = new Json(obj);
		json.setOuterJson(outerJson);
		return jsonToResource(json);
	}

	public static List<Resource> makeResources(JSONArray array) {
		Json json = new Json(array);
		List<Resource> result = new ArrayList<Resource>();
		for(Object obj : array) {
			if(obj instanceof String) {
				//add this 
				String namespace = (String) obj;
				Object refObj = json.get(namespace);
				if(refObj instanceof JSONArray) {
					for(Resource r : makeResources((JSONArray)refObj)) {
						result.add(r);
					}
				} else {
					result.add(makeResource((JSONObject)refObj));
				}
			} else {
				result.add(makeResource((JSONObject) obj));
			}
		}
		return result;
	}

	public static List<Resource> makeResources(JSONArray array, Json outerJson) {
		Json json = new Json(array);
		List<Resource> result = new ArrayList<Resource>();
		for(Object obj : array) {
			if(obj instanceof String) {
				//add this 
				String namespace = (String) obj;
				Object refObj = json.get(namespace);
				if(refObj instanceof JSONArray) {
					for(Resource r : makeResources((JSONArray)refObj, outerJson)) {
						result.add(r);
					}
				} else {
					result.add(makeResource((JSONObject)refObj, outerJson));
				}
			} else {
				result.add(makeResource((JSONObject) obj, outerJson));
			}
		}
		return result;
	}

	public static List<Resource> makeResources(String filename) {
		List<Resource> result = new ArrayList<Resource>();
		InputStream is = Json.openInputStream(filename);
		Reader reader = new InputStreamReader(is);
		Object obj = JSONValue.parse(reader);
		if(obj == null) throw new InvalidJsonException("Invalid json.");
		if(obj instanceof JSONObject) {
			result.add(makeResource((JSONObject) obj));
		} else if(obj instanceof JSONArray) {
			result = makeResources((JSONArray) obj);
		}
		try {
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}
	
}
