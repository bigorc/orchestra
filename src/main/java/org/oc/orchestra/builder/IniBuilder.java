package org.oc.orchestra.builder;

import java.util.Iterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.oc.json.Json;
import org.oc.orchestra.resource.Ini;
import org.oc.orchestra.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IniBuilder implements Builder {
	private final Logger logger = LoggerFactory.getLogger(IniBuilder.class);
	@Override
	public Resource makeResource(Json json) {
		Ini ini = null;
	
		String filename = (String) json.get("file");
		logger.debug("file:" + filename);
		
		Object multi = json.get("multiValue");
		boolean multiValue = false;
		if(multi != null) {
			multiValue = json.get("multiValue").toString().equalsIgnoreCase("True")
					? true : false;
			logger.debug("multiValue:" + multiValue);
		}
		
		String operator = (String) json.get("operator");
		logger.debug("operator:" + operator);
		
		String comment = (String) json.get("comment");
		logger.debug("comment:" + comment);
		
		ini = new Ini(filename, multiValue, operator, comment);
		if("true".equalsIgnoreCase((String) json.get("block"))) ini.setBlock(true);
		logger.debug("block:" + json.get("block"));
		
		ini.setClient((String) json.get("client"));
		logger.debug("client:" + json.get("client"));
		
		if(json.get("contain_properties") != null) {
			JSONArray array = (JSONArray) json.get("contain_properties");
			Iterator<?> it = array.iterator();
			while(it.hasNext()) {
				JSONObject obj = (JSONObject) it.next();
				String section = (String) obj.get("section");
				if(section != null && section.startsWith("$")) 
					section = (String) json.get(section.substring(1));
				String name = (String) obj.get("name");
				if(name != null && name.startsWith("$")) 
					name = (String) json.get(name.substring(1));
				String value = (String) obj.get("value");
				if(value != null && value.startsWith("$")) 
					value = (String) json.get(value.substring(1));
				ini.addContainProperties(section, name, value);
				logger.debug("contain_properties:" + section + "," + name + "," + value);
			}
		}
		if(json.get("not_contain_properties") != null) {
			JSONArray array = (JSONArray) json.get("not_contain_properties");
			Iterator<?> it = array.iterator();
			while(it.hasNext()) {
				JSONObject obj = (JSONObject) it.next();
				String section = (String) obj.get("section");
				if(section != null && section.startsWith("$")) 
					section = (String) json.get(section.substring(1));
				String name = (String) obj.get("name");
				if(name != null && name.startsWith("$")) 
					name = (String) json.get(name.substring(1));
				String value = (String) obj.get("value");
				if(value != null && value.startsWith("$")) 
					value = (String) json.get(value.substring(1));
				ini.addNotContainProperties(section, name, value);
				logger.debug("not_contain_properties:" + section + "," + name + "," + value);
			}
		}
		return ini;
	}

}
