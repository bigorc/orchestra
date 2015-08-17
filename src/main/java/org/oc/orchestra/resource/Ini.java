package org.oc.orchestra.resource;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.ini4j.InvalidFileFormatException;
import org.ini4j.Profile.Section;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.oc.orchestra.client.Client;

public class Ini extends Configuration {
	private String operator = "=";
	private String comment = "#";
	org.ini4j.Ini ini;
	private String filename;
	private List<Property> contain_properties = new ArrayList<Property>();
	private List<Property> not_contain_properties = new ArrayList<Property>();
	
	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	
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

	public Ini(String filename, boolean multiValue, String operator,
			String comment) {
		super(multiValue);
		this.filename = filename;
		if(operator != null) {
			this.operator = operator;
		}
		if(comment != null) {
			this.comment = comment;
		}
	}

	public Ini(String filename) {
		this.filename = filename;
	}
	
	public Ini(String filename, boolean multiValue) {
		super(multiValue);
		this.filename = filename;
	}


	public void init() {
		if(ini != null) return;
		try {
			ini = new org.ini4j.Ini(new File(filename), operator, operator.charAt(0), 
					comment, comment.charAt(0));
		} catch (InvalidFileFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public String getOperator() {
		return operator;
	}

	public void setOperator(String operator) {
		this.operator = operator;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	@Override
	public boolean contains(String name, String value) {
		Section section = ini.get("?");
		if(section == null) return false;
		if(allowsMultiValue()) {
			List vl = section.getAll(name);
			if(vl == null) return false;
			return vl.contains(value);
		} else {
			Object obj = section.get(name);
			if(obj == null ) return false;
			return obj.equals(value);
		}
		
	}

	public boolean contains(String section, String name, String value) {
		if(ini.get(section) == null) return false;
//		if(value == null) {//flag
//			return ini.get(section).containsKey(name);
//		}
		if(allowsMultiValue()) {
			List vl = ini.get(section).getAll(name);
			if(vl == null) return false;
			return vl.contains(value);
		} else {
			Object obj = ini.get(section).get(name);
			if(obj == null ) {
				if(value == null)return true;
				return false;
			}
			return obj.equals(value);
		}
		
	}
	@Override
	public boolean containsKey(String key) {
		Section section = ini.get("?");
		if(section == null) return false;
		return section.containsKey(key);
	}

	public boolean containsKey(String section, String key) {
		Section section2 = ini.get(section);
		if(section2 == null) return false;
		return section2.containsKey(key);
	}

	@Override
	public void add(String name, String value) {
		if(contains(name, value))return;
		Section section = ini.get("?");
		if(section == null) return;
		if(allowsMultiValue()) {
			section.add(name, value);
		} else {
			section.put(name, value);
		}
		try {
			ini.store();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void add(String section, String name, String value) {
		if(section == null) section = "?";
		if(contains(section, name, value)) return;
		if(allowsMultiValue()) {
			ini.add(section, name, value);
		} else {
			ini.put(section, name, value);
		}
		try {
			ini.store();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void remove(String name, String value) {
		if(!contains(name, value))return;
		if(allowsMultiValue()) {
			ini.get("?").remove(name, 
					ini.get("?").getAll(name).indexOf(value));
		} else {
			ini.get("?").remove(name);
		}
		try {
			ini.store();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void remove(String section, String name, String value) {
		if(section == null) section = "?";
		if(!contains(section, name, value))return;
		if(allowsMultiValue()) {
			ini.get(section).remove(name, 
					ini.get(section).getAll(name).indexOf(value));
		} else {
			ini.get(section).remove(name);
		}
		try {
			ini.store();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	@Override
	public void realize() {
		init();
		if(client == null || client.equals(Client.getName())) {
			for(Property p : contain_properties) {
				add(p.section, p.name, p.value);
			}

			for(Property p : not_contain_properties) {
				remove(p.section, p.name, p.value);
			}
		} else {
			coordinator.assignResourceTask(this);
		}
	}

	@Override
	public String getCurrentState() {
		init();
		if(client == null || client.equals(Client.getName())) {
			for(Property p : contain_properties) {
				if(!contains(p.section, p.name, p.value)) return "not_configured";
			}

			for(Property p : not_contain_properties) {
				if(contains(p.section, p.name, p.value)) return "not_configured";
			}
			return "configured";
		} else {
			return coordinator.getState(getUri(), toRO());
		}
	}
	
	public void addContainProperties(String section, String name, String value) {
		if(section == null) section = "?";
		contain_properties.add(new Property(section, name, value));
	}
	
	public void addNotContainProperties(String section, String name, String value) {
		if(section == null) section = "?";
		not_contain_properties.add(new Property(section, name, value));
	}
	
	public class Property {
		public String section;
		public String name;
		public String value;
		
		public Property(String section, String name, String value) {
			this.section = section;
			this.name = name;
			this.value = value;
		}
	}

	@Override
	public String uri() {
		return getClass().getName() + "_" + filename.replace('/', '\\');
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
		if(!operator.equals("=")) json.put("operator", operator);
		if(!comment.equals("#")) json.put("comment", comment);
		JSONArray contain = new JSONArray();
		json.put("contain_properties", contain);
		for(Property p : contain_properties) {
			JSONObject obj = new JSONObject();
			contain.add(obj);
			obj.put("section", p.section);
			obj.put("name", p.name);
			obj.put("value", p.value);
		}
		
		JSONArray not_contain = new JSONArray();
		json.put("not_contain_properties", not_contain);
		for(Property p : not_contain_properties) {
			JSONObject obj = new JSONObject();
			not_contain.add(obj);
			obj.put("section", p.section);
			obj.put("name", p.name);
			obj.put("value", p.value);
		}
		return json;
	}
}
