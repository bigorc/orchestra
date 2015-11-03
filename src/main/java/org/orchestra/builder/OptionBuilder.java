package org.orchestra.builder;

import java.util.Iterator;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.orchestra.json.Json;
import org.orchestra.resource.Option;
import org.orchestra.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OptionBuilder implements Builder {
	private final Logger logger = LoggerFactory.getLogger(OptionBuilder.class);
	@Override
	public Resource makeResource(Json json) {
		Option option = null;
		
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
		
		option = new Option(filename, operator);
		option.setMultiValue(multiValue);
		
		if("true".equalsIgnoreCase((String) json.get("block"))) option.setBlock(true);
		logger.debug("block:" + json.get("block"));
		
		option.setClient((String) json.get("client"));
		logger.debug("client:" + json.get("client"));
		
		if(json.get("contain_properties") != null) {
			JSONArray array = (JSONArray) json.get("contain_properties");
			Iterator<?> it = array.iterator();
			while(it.hasNext()) {
				JSONObject obj = (JSONObject) it.next();
				String name = (String) obj.get("name");
				if(name != null && name.startsWith("$")) 
					name = (String) json.get(name.substring(1));
				String value = (String) obj.get("value");
				if(value != null && value.startsWith("$")) 
					value = (String) json.get(value.substring(1));
				
				if(value != null) {
					option.addContainProperties(name, value.toString());
				} else {
					option.addContainProperties(name, null);
				}
				logger.debug("contain_properties:" +  name + "," + value);
			}
		}
		if(json.get("not_contain_properties") != null) {
			JSONArray array = (JSONArray) json.get("not_contain_properties");
			Iterator<?> it = array.iterator();
			while(it.hasNext()) {
				JSONObject obj = (JSONObject) it.next();
				String name = (String) obj.get("name");
				String value = (String) obj.get("value");
				option.addNotContainProperties(name, value);
				logger.debug("not_contain_properties:" +  name + "," + value);
			}
		}
		return option;
	}

}
