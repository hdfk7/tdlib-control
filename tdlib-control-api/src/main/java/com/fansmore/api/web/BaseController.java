package com.fansmore.api.web;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public abstract class BaseController {
    protected static final Logger log = LoggerFactory.getLogger(BaseController.class);

    @Autowired
    protected HttpServletRequest request;

    @Resource
    protected HttpServletResponse response;

    String queryAuthToken() {
        String authorization = request.getHeader("Authorization");
        if (StringUtils.isEmpty(authorization)) {
            return request.getParameter("token");
        }
        return authorization;
    }
}
