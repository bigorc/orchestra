package org.oc.orchestra.resource;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.ini4j.Options;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.oc.orchestra.client.Client;
import org.oc.orchestra.resource.Ini.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Option extends Configuration {
	private final Logger logger = LoggerFactory.getLogger(Option.class);
	private Options option;
	private String filename;
	private String operator;

	private List<Property> contain_properties = new ArrayList<Property>();
	private List<Property> not_contain_properties = new ArrayList<Property>();
	
	public List<Property> getContain_properties() {
		return contain_properties;
	}

	public void setContain_properties(List<Property> contain_properties) {
		this.contain_properties = contain_properties;
	}

	public List<Property> getNot_contain_properties() {
		return not_contain_properties;
	}

	public void setNot_contain_properties(List<Property> not_contain_properties) {
		this.not_contain_properties = not_contain_properties;
	}
	public Options getOption() {
		return option;
	}

	public void setOption(Options option) {
		this.option = option;
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public String getOperator() {
		return operator;
	}

	public void setOperator(String operator) {
		this.operator = operator;
	}

	public Option(String filename) {
		this.filename = filename;
		
	}
	
	public Option(String filename, String operator) {
		this.filename = filename;
		this.operator = operator;
	}
	
	@Override
	public boolean containsKey(String key) {
		return option.containsKey(key);
	}

	@Override
	public boolean contains(String key, String value) {
		if(allowsMultiValue()) {
			String[] va = option.getAll(key, String[].class);
			if(va.length == 0) return false;
			for(String v : va) {
				if(v.equals(value)) return true; 
			}
			return false;
		} else {
			String v = option.get(key);
			if(v == null) return false;
			if(v.equals(value))	return true;
			return false;
		}
	}

	@Override
	public void add(String key, String value) {
		if(contains(key, value)) return;
		if(allowsMultiValue()) {
			option.add(key, value);
		} else {
			option.put(key, value);
		}
		try {
			option.store();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void remove(String key, String value) {
		if(!contains(key, value)) return;
		if(allowsMultiValue()) {
			option.remove(key, option.getAll(key).indexOf(value));
		} else {
			option.remove(key);
		}
		try {
			option.store();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void init() {
		try {
			if(operator == null) {
				option = new Options(new File(filename));
			} else {
				option = new Options(new File(filename), operator);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void realize() {
		logger.info("Realizing option resource " + filename);
		init();
		if(client == null || client.equals(Client.getName())) {
			for(Property p : contain_properties) {
				add(p.name, p.value);
			}

			for(Property p : not_contain_properties) {
				remove(p.name, p.value);
			}
		} else {
			if(block) {
				Client.getCoordinator(client).assignResourceTask(this);
			} else {
				Client.getCoordinator(client).asyncAssignResourceTask(this);
			}
		}
	}

	@Override
	public String getCurrentState() {
		init();
		if(client == null || client.equals(Client.getName())) {
			for(Property p : contain_properties) {
				if(!contains(p.name, p.value)) return "not_configured";
			}

			for(Property p : not_contain_properties) {
				if(contains(p.name, p.value)) return "not_configured";
			}
			return "configured";
		} else {
			return Client.getCoordinator(client).getState(uri(), toRO());
		}
	}


	public void addContainProperties(String name, String value) {
		contain_properties.add(new Property(name, value));
	}
	
	public void addNotContainProperties(String name, String value) {
		not_contain_properties.add(new Property(name, value));
	}
	
	public class Property {
		public String name;
		public String value;
		
		public Property(String name, String value) {
			this.name = name;
			this.value = value;
		}
	}
	
	@Override
	public String uri() {
		return "option_" + filename.replace('/', '\\');
	}
	
	@Override
	public String toRO() {
		return toJson().toJSONString();
	}

	@Override
	public JSONObject toJson() {
		JSONObject json = new JSONObject();
		json.put("type", "ini");
		json.put("file", filename);
		if(allowsMultiValue()) json.put("multiValue", "True");
		if(operator != null) json.put("operator", operator);
		JSONArray contain = new JSONArray();
		json.put("contain_properties", contain);
		for(Property p : contain_properties) {
			JSONObject obj = new JSONObject();
			contain.add(obj);
			obj.put("name", p.name);
			obj.put("value", p.value);
		}
		
		JSONArray not_contain = new JSONArray();
		json.put("not_contain_properties", not_contain);
		for(Property p : not_contain_properties) {
			JSONObject obj = new JSONObject();
			not_contain.add(obj);
			obj.put("name", p.name);
			obj.put("value", p.value);
		}
		return json;
	}
}
