package org.oc.orchestra.builder;

import java.util.Iterator;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.oc.json.Json;
import org.oc.orchestra.ResourceFactory;
import org.oc.orchestra.resource.Configuration;
import org.oc.orchestra.resource.Ini;
import org.oc.orchestra.resource.Resource;

public class IniBuilder implements Builder {

	@Override
	public Resource makeResource(Json json) {
		Ini ini = null;
	
		String filename = (String) json.get("file");
		Object multi = json.get("multiValue");
		boolean multiValue = false;
		if(multi != null) {
			multiValue = json.get("multiValue").toString().equalsIgnoreCase("True")
					? true : false;
		}
		String operator = (String) json.get("operator");
		String comment = (String) json.get("comment");
		ini = new Ini(filename, multiValue, operator, comment);
		
		if(json.get("contain_properties") != null) {
			JSONArray array = (JSONArray) json.get("contain_properties");
			Iterator it = array.iterator();
			while(it.hasNext()) {
				JSONObject obj = (JSONObject) it.next();
				String section = (String) obj.get("section");
				String name = (String) obj.get("name");
				String value = (String) obj.get("value");
				ini.addContainProperties(section, name, value);
			}
		}
		if(json.get("not_contain_properties") != null) {
			JSONArray array = (JSONArray) json.get("not_contain_properties");
			Iterator it = array.iterator();
			while(it.hasNext()) {
				JSONObject obj = (JSONObject) it.next();
				String section = (String) obj.get("section");
				String name = (String) obj.get("name");
				String value = (String) obj.get("value");
				ini.addNotContainProperties(section, name, value);
			}
		}
		return ini;
	}

}
