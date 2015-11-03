/*
 * Copyright 2012 Michael Morello
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.orchestra.provider;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.util.EntityUtils;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.ZooDefs.Perms;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Id;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.orchestra.client.Client;
import org.orchestra.client.HttpCommand;
import org.orchestra.client.HttpCommandBuilder;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class ACLProvider implements
org.apache.curator.framework.api.ACLProvider {

	
	public List<ACL> getDefaultAcl() {
		throw new NotImplementedException();
	}

	public List<ACL> getAclForPath(String path) {
		String clientname = path.substring(path.lastIndexOf('/') + 1);
		Client client = new Client();
		return client.getAclList(clientname);
	}

}
