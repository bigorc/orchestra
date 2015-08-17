package org.oc.util;

import java.util.Map;

public class MapUtil {

	public static void mergeToLeft(Map left, Map right) {
		if(right == null) return;
		for(Object el : left.keySet()) {
			if(right.containsKey(el)) {
				left.put(el, right.get(el));
			}
		}
	}
}
