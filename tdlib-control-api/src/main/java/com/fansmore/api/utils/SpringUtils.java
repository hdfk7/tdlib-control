package com.fansmore.api.utils;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;

@Configuration
public class SpringUtils implements ApplicationContextAware {

    private static ApplicationContext context;

    @Override
    public void setApplicationContext(@NonNull ApplicationContext var) throws BeansException {
        SpringUtils.context = var;
    }

    public static ApplicationContext getContext() {
        return context;
    }

    public static Object getBean(String var) throws BeansException {
        return context.getBean(var);
    }

    public static <T> T getBean(String var1, Class<T> var2) throws BeansException {
        return context.getBean(var1, var2);
    }

    public static <T> T getBean(Class<T> var) throws BeansException {
        return context.getBean(var);
    }

    public static String getServerId() {
        return getContext().getEnvironment().getProperty("server.id");
    }

    public static <T> T getProperty(String var1, Class<T> var2, T defaultValue) {
        T value = SpringUtils.getContext().getEnvironment().getProperty(var1, var2);
        if (value == null) value = defaultValue;
        return value;
    }

    public static <T> T getProperty(String var1, Class<T> var2) {
        return getProperty(var1, var2, null);
    }
}
