package org.oc.json;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.oc.orchestra.client.HttpCommand;
import org.oc.orchestra.client.HttpCommandBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resource States Transferring Object
 * @author fuhw
 *
 */
public class Json {
	private JSONObject root;
	private JSONArray rootArray;
	private Set parentKeySet = null;
	private Set keySet = null;
	
	private static final Logger LOG = LoggerFactory.getLogger(Json.class);
	
	public static final String orchestra_server = "localhost:8111";
	public static final String pro_path = "pro/";
//	public static final String orchestra_port = "8111";
	
	public Json(JSONObject root) {
		this.root = root;
	}
	
	public Json(String filename) {
		InputStream is = ClassLoader.getSystemResourceAsStream(filename);
		if(is == null) {
			downloadFileIfNeeded(filename);
			is = ClassLoader.getSystemResourceAsStream(filename);
		}
		Reader reader = new InputStreamReader(is);
		Object obj = JSONValue.parse(reader);
		if(obj instanceof JSONObject) this.root = (JSONObject) obj;
		if(obj instanceof JSONArray) this.rootArray = (JSONArray) obj;
	}

	public static void downloadFileIfNeeded(String filename) {
		File file = new File(FileUtils.getUserDirectoryPath() + "/.pro/" + filename);
		FileOutputStream fos = null;
		InputStream is = null;
		
		if(!file.exists()) {
			try {
				fos = FileUtils.openOutputStream(file);
				is = openInputStream(filename);
				int ch = 0;
				while((ch=is.read()) != -1){  
		            fos.write(ch);  
		        }
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					is.close();
					fos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
			}
		}
	}

	public static InputStream openInputStream(String filename) throws ClientProtocolException, IOException {
        String username = "admin";
		String password = "admin";
		String host = "orchestra";
		int port = 8183;
		HttpCommand cmd = new HttpCommandBuilder(username, password)
			.setHost(host)
			.setScheme("https")
			.setPort(port)
			.setAction("read")
			.setTarget("pro")
			.setParameter("filename", "test/nif-eth1.json")
			.build();
		HttpResponse response = cmd.execute();
        InputStream in = response.getEntity().getContent();
        return in;
	}

	public Json(JSONArray array) {
		this.rootArray = array;
	}

	//the principle of "lazy references"
	public Object get(String namespace) {
		if(namespace.startsWith("$'")) {
			return resolveReference(namespace);
		}
		Object result = getLocalParam(namespace);
		
		if(result == null && root != null) {
			//param not in local json, local json isn't an array
			if(root.containsKey("$parent")) {
				result = lookupParent(namespace);
				if(result != null) return result;
			}
			if(root.containsKey("$parents")) {
				result = lookupParents(namespace);
				if(result != null) return result;
			}
			
		}
		return result;
	}

	public Object lookupParents(String namespace) {
		Object result = null;
		JSONArray array = (JSONArray) root.get("$parents");
		for(int i = array.size() - 1;i >= 0;i--) {
			result = lookupJson(namespace, array.get(i).toString());
			if(result != null) return result;
		}
		return null;
	}
	
	public Object lookupParent(String namespace) {
		String filename = root.get("$parent").toString();
		return lookupJson(namespace, filename);
	}

	public Object lookupJson(String namespace, String filename) {
		Json json = new Json(filename);
		return json.get(namespace);
	}

	public Object getLocalParam(String namespace) {
		LOG.debug("Fetching parameter " + namespace);
		
		Object current = (root == null) ? rootArray : root;

		expandArrayIterator(current);
		
		String[] names = breakNamespace(namespace);
		for(String name: names) {
			String[] indexedName = breakIndex(name);
			if(indexedName == null) {//not an array
				if(current instanceof JSONObject) {
					current = ((JSONObject) current).get(name);
					expandArrayIterator(current);
				}
				if(current instanceof String && current.toString().startsWith("$")) {
					current = resolveReference(current);
				}
			} else {
				JSONArray array = null;
				if(current instanceof JSONObject) {
					array = (JSONArray) ((JSONObject) current).get(indexedName[0]);
					for(int i = 1;i < indexedName.length - 1;i++) {
						array = (JSONArray) array.get(Integer.valueOf(indexedName[i]));
					}
				} else {
					array = (JSONArray) current;
				}
				current = array.get(Integer.valueOf(indexedName[indexedName.length - 1]));
			}
		}
		//see if the fetched object is a namespace string before return it
		if(current instanceof String && current.toString().startsWith("$")) {
			return resolveReference(current);
		}
		return current;
	}

	public void expandArrayIterator(Object current) {
		Map arrits = new HashMap();
		//expand array comprehensions here
		if(current instanceof JSONObject) {
			JSONObject obj = (JSONObject)current;
			
			Iterator it = obj.keySet().iterator();
			while(it.hasNext()) {
				Object k = it.next();
				if(isArrayIterator(k)) {
					arrits.put(k, ((JSONObject) current).get(k));
					it.remove();
				}
			}

			it = arrits.keySet().iterator();
			while(it.hasNext()) {
				Object k = it.next();
				System.out.println(k);
				//handle key expansion
				List<String> keys = new ArrayList<String>();
				List<String> values = new ArrayList<String>();
				String key = ((String)k);
				Object value = arrits.get(k);
				
				Object arrkey = get(key.substring(2, key.length() - 1));
				
				if(arrkey instanceof JSONArray) {
					for(Object o : (JSONArray)arrkey) {
						keys.add(o.toString());
					}
				} else {
					throw new RuntimeException("namespace of array iterator must refer to an array.");
				}
				
				Object  arrval = null;
				//handle value expansion
				if(value instanceof JSONObject) {
					throw new RuntimeException("namespace of array iterator must refer to an array.");
				} else if(value instanceof JSONArray || isArrayIterator(value)) {
					if(value instanceof JSONArray) {
						arrval = value;
					} else {
						String val = (String) value;
						arrval = get(val.substring(2, val.length() - 1));
						if(!(arrval instanceof JSONArray))
							throw new RuntimeException("namespace of array iterator must refer to an array.");
					}
					for(Object o : (JSONArray)arrval) {
						values.add(o.toString());
					}
					if(keys.size() != values.size())
						throw new RuntimeException("size of value array does not match size of key array.");
				} else {
					arrval = new JSONArray();
					for(int i = 0;i < keys.size();i++)
						((JSONArray)arrval).add(value);
				}
				for(int i = 0;i < keys.size();i++) {
					((JSONObject) current).put(keys.get(i), ((JSONArray)arrval).get(i));
				}
			}
			
		}
	}

	public boolean isArrayIterator(Object obj) {
		if(!(obj instanceof String)) return false;
		String str = obj.toString();
		return str.startsWith("[$") && str.endsWith("]");
	}

	public Object resolveReference(Object current) {//current has to be a string
		if(current.toString().startsWith("$'")) {//might be a file
			String regex = "\\$'(.+)'(.*)";
			Pattern pattern = Pattern.compile(regex);
			Matcher matcher = pattern.matcher(current.toString());
			if (matcher.find()) {
				String filename = matcher.group(1);
				Json json = new Json(filename);
				String namespace = matcher.group(2);
				if(!namespace.equals("")) {//namespace must start with a "."
					return json.get(namespace.substring(1));
				} else {
					return json.getRoot() == null ? json.getRootArray() : json.getRoot();
				}
			}
		}
		return get(current.toString().substring(1));
	}

	public String getParamAsString(String namespace) {
		Object result = get(namespace);
		if(result instanceof String) return result.toString();
		return null;
	}

	public String[] matcheNamespace(String[] names) {
		String[] ns = breakNamespace(root.get("$namespace").toString());
		for(int i = 0;i < ns.length;i++) {
			if(!ns[i].equals(names[i])) return null;
		}
		return Arrays.copyOfRange(names, ns.length, names.length);
	}

	/**
	 * 
	 * @param name
	 * @return 
	 */
	public static String[] breakIndex(String name) {
		if(!Pattern.compile(".*(\\[\\d+\\])+").matcher(name).find()) {
			return null; //no match
		}
		String regex = "(\\[\\d+\\])+$";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(name);
		ArrayList<String> result = new ArrayList<String>();
		
		if(matcher.find()) {
			String arrayname = name.substring(0, matcher.start());
			//in case someone wants to use ab[c] as array name
			arrayname = arrayname.replace("\\[", "[");
			arrayname = arrayname.replace("\\]", "]");
			result.add(arrayname);
			
			matcher = Pattern.compile("\\d+").matcher(matcher.group(0));
			while(matcher.find()) {
				String digit = matcher.group(0);
				result.add(digit);
			}
		}
		return result.toArray(new String[result.size()]);
	}

	public static String[] breakNamespace(String namespace) {
		String[] names = namespace.split("\\.");
		String[] result = new String[names.length];
		int resultLength = 0;
		for(int i = 0;i < names.length;i++) {
			if(!names[i].endsWith("\\")) {
				result[resultLength] = names[i];
				resultLength++;
			} else {
				String nameWithDots = names[i].substring(0, names[i].length() - 1) + ".";
				
				for(int j = i + 1;j < names.length;j++) {
					i++;
					if(!names[j].endsWith("\\")) {
						nameWithDots += names[j];
						break;
					} else {
						nameWithDots += names[j].substring(0, names[j].length() - 1);
						nameWithDots += ".";
					}
				}
				result[resultLength] = nameWithDots;
				resultLength++;
			}
		}
		return Arrays.copyOf(result, resultLength);
	}

	public Set keySet() {
		if(keySet != null) return keySet;
		keySet = new HashSet();
		for(Object key : root.keySet()) {
			if(!key.toString().equals("$parent") && 
				!key.toString().equals("$parents"))
				keySet.add(key);
		}
		keySet.addAll(parentKeySet());
		return keySet;
	}
	
	public Set parentKeySet() {
		if(parentKeySet != null) return parentKeySet;
		parentKeySet = new HashSet();
		if(root.containsKey("$parent")) {
			Json parent= new Json((String)root.get("$parent"));
			parentKeySet.addAll(parent.keySet());
		}
		if(root.containsKey("$parents")) {
			JSONArray array = (JSONArray) root.get("$parents");
			
			for(int i = 0;i < array.size();i++) {
				Json parent = new Json((String) array.get(i));
				parentKeySet.addAll(parent.keySet());
			}
		}
		return parentKeySet;
	}
	
	public JSONObject getRoot() {
		return root;
	}

	public JSONArray getRootArray() {
		return rootArray;
	}

}
