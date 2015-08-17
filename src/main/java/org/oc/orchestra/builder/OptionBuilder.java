package org.oc.orchestra.builder;

import java.util.Iterator;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.oc.json.Json;
import org.oc.orchestra.resource.Ini;
import org.oc.orchestra.resource.Option;
import org.oc.orchestra.resource.Resource;

public class OptionBuilder implements Builder {

	@Override
	public Resource makeResource(Json json) {
		Option option = null;
		
		String filename = (String) json.get("file");
		Object multi = json.get("multiValue");
		boolean multiValue = false;
		if(multi != null) {
			multiValue = json.get("multiValue").toString().equalsIgnoreCase("True")
					? true : false;
		}
		String operator = (String) json.get("operator");
		option = new Option(filename, operator);
		option.setMultiValue(multiValue);
		
		if(json.get("contain_properties") != null) {
			JSONArray array = (JSONArray) json.get("contain_properties");
			Iterator it = array.iterator();
			while(it.hasNext()) {
				JSONObject obj = (JSONObject) it.next();
				String name = (String) obj.get("name");
				Object value = obj.get("value");
				if(value != null) {
					option.addContainProperties(name, value.toString());
				} else {
					option.addContainProperties(name, null);
				}
			}
		}
		if(json.get("not_contain_properties") != null) {
			JSONArray array = (JSONArray) json.get("not_contain_properties");
			Iterator it = array.iterator();
			while(it.hasNext()) {
				JSONObject obj = (JSONObject) it.next();
				String name = (String) obj.get("name");
				String value = (String) obj.get("value");
				option.addNotContainProperties(name, value);
			}
		}
		return option;
	}

}
