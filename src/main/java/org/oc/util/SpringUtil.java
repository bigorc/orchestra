package org.oc.util;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class SpringUtil {
	private static ApplicationContext context;
	
	public static Object getBean(String beanName) {
		if(context == null) {
			context = new ClassPathXmlApplicationContext("spring.xml");
		}
		return context.getBean(beanName);
	}
}
