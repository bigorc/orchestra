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

package org.oc.orchestra.coordinate;

import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.data.Id;
import org.apache.zookeeper.server.ServerCnxn;
import org.apache.zookeeper.server.auth.AuthenticationProvider;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;

public class ZookeeperAuthenticationProvider implements AuthenticationProvider {

  
  public String getScheme() {
    return "orchestra";
  }

  public Code handleAuthentication(ServerCnxn cnxn, byte[] authData) {
    String userName = new String(authData, Charsets.UTF_8);
    System.out.println(userName);
    // A non null or empty user name must be provided
    if (!Strings.isNullOrEmpty(userName) && userName.equals("client1")) {
      // This line is VERY important ! return code is not enough
      cnxn.addAuthInfo(new Id(getScheme(), userName));
      return Code.OK;
    }
    return Code.AUTHFAILED;
  }

  public boolean matches(String id, String aclExpr) {
    return true;
  }

  public boolean isAuthenticated() {
    return true;
  }

  public boolean isValid(String id) {
   return true;
  }

}
