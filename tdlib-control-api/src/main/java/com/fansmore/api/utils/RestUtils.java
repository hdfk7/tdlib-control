/*
 * Copyright (C), 2018-2019, 四川益触云科技有限公司
 * FileName: RestTemplateTool
 * Author:   yiChuYun_lq
 * Date:     2019/6/22 21:03
 * Description:
 * History:
 */
package com.fansmore.api.utils;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

/**
 * @author yiChuYun_lq
 * @date 2019/6/22 21:03
 * @since 1.0.0
 */
public class RestUtils {

    private static final RestTemplate restTemplate;

    static {
        if (SpringUtils.getContext() == null) {
            restTemplate = new RestTemplate();
        } else {
            restTemplate = SpringUtils.getBean("restTemplate", RestTemplate.class);
        }
    }

    public static <T> T get(String url, Class<T> t, HttpHeaders h) {
        if (h == null) {
            h = new HttpHeaders();
        }
        HttpEntity<Object> httpEntity = new HttpEntity<>(null, h);
        return restTemplate.exchange(url, HttpMethod.GET, httpEntity, t).getBody();
    }

    public static <T> T get(String url, Class<T> t) {
        return get(url, t, null);
    }

    public static <T> T post(String url, Class<T> t, Object o, HttpHeaders h) {
        if (h == null) {
            h = new HttpHeaders();
        }
        HttpEntity<Object> e = new HttpEntity<>(o, h);
        return restTemplate.postForObject(url, e, t);
    }

    public static <T> T post(String url, Class<T> t, Object o) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        return post(url, t, o, h);
    }

    public static <T> T postJson(String url, Class<T> t, Object o, HttpHeaders headers) {
        if (headers == null) {
            headers = new HttpHeaders();
        }
        headers.setContentType(MediaType.APPLICATION_JSON);
        return post(url, t, o, headers);
    }

    public static <T> T postJson(String url, Class<T> t, Object o) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        return postJson(url, t, o, h);
    }

    public static <T> T put(String url, Class<T> t, Object o, HttpHeaders h) {
        if (h == null) {
            h = new HttpHeaders();
        }
        HttpEntity<Object> httpEntity = new HttpEntity<>(o, h);
        return restTemplate.exchange(url, HttpMethod.PUT, httpEntity, t).getBody();
    }

    public static <T> T put(String url, Class<T> t, Object o) {
        return put(url, t, o, null);
    }

    public static <T> T putJson(String url, Class<T> t, Object o, HttpHeaders headers) {
        if (headers == null) {
            headers = new HttpHeaders();
        }
        headers.setContentType(MediaType.APPLICATION_JSON);
        return put(url, t, o, headers);
    }

    public static <T> T putJson(String url, Class<T> t, Object o) {
        return putJson(url, t, o, null);
    }


    public static <T> T delete(String url, Class<T> t, Object o, HttpHeaders h) {
        if (h == null) {
            h = new HttpHeaders();
        }
        HttpEntity<Object> httpEntity = new HttpEntity<>(o, h);
        return restTemplate.exchange(url, HttpMethod.DELETE, httpEntity, t).getBody();
    }

    public static <T> T delete(String url, Class<T> t, Object o) {
        return delete(url, t, o, null);
    }

    public static <T> T deleteJson(String url, Class<T> t, Object o, HttpHeaders headers) {
        if (headers == null) {
            headers = new HttpHeaders();
        }
        headers.setContentType(MediaType.APPLICATION_JSON);
        return delete(url, t, o, headers);
    }

    public static <T> T deleteJson(String url, Class<T> t, Object o) {
        return deleteJson(url, t, o, null);
    }
}
