package com.fansmore.api.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JSONUtils {
    private static final ObjectMapper mapper;

    static {
        if (SpringUtils.getContext() == null) {
            mapper = new ObjectMapper();
            mapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        } else {
            mapper = SpringUtils.getBean(ObjectMapper.class);
        }
    }

    public static String toJSONString(Object obj) {
        try {
            if (obj instanceof String) return (String) obj;
            return mapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.trace(e.getMessage(), e);
        }
        return "{}";
    }

    public static <T> T toObject(String json, Class<T> type) {
        try {
            return mapper.readValue(json, type);
        } catch (Exception e) {
            log.trace(e.getMessage(), e);
        }
        return null;
    }

    public static <T> T toObject(String json, TypeReference<T> reference) {
        try {
            return mapper.readValue(json, reference);
        } catch (Exception e) {
            log.trace(e.getMessage(), e);
        }
        return null;
    }

    public static <T> T toObject(Object json, Class<T> type) {
        return toObject(toJSONString(json), type);
    }

    public static <T> T toObject(Object json, TypeReference<T> reference) {
        return toObject(toJSONString(json), reference);
    }
}
