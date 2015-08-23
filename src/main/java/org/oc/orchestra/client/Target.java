package org.oc.orchestra.client;

import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONValue;

public abstract class Target {

	protected HttpCommandBuilder builder;

	public abstract void execute(String method, CommandLine cmd);
	public void output(HttpResponse response) {
		HttpEntity resEntity = response.getEntity();

	    System.out.println(response.getStatusLine());
	    if (resEntity != null) {
	    	String str = null;
	    	try {
	    		str = EntityUtils.toString(resEntity);
	    	} catch (ParseException | IOException e) {
	    		e.printStackTrace();
	    	}
	    	Object json = JSONValue.parse(str);
	    	if(json != null) {
	    		System.out.println(JSONValue.toJSONString(json));
	    	} else {
	    		System.out.println(str);
	    	}
	    }
	    if (resEntity != null) {
	    	try {
	    		resEntity.consumeContent();
	    	} catch (IOException e) {
	    		e.printStackTrace();
	    	}
	    }
	}

}
