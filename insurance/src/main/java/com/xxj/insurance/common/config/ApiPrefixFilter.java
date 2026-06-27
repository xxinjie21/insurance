package com.xxj.insurance.common.config;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiPrefixFilter extends OncePerRequestFilter {

    private static final String API_PREFIX = "/api";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String path = uri.substring(contextPath.length());

        if (path.startsWith(API_PREFIX)) {
            String strippedPath = path.substring(API_PREFIX.length());
            if (strippedPath.isEmpty()) {
                strippedPath = "/";
            }
            final String newPath = strippedPath;
            String newUri = contextPath + newPath;
            HttpServletRequest wrapper = new HttpServletRequestWrapper(request) {
                @Override
                public String getRequestURI() {
                    return newUri;
                }

                @Override
                public String getServletPath() {
                    return newPath;
                }
            };
            chain.doFilter(wrapper, response);
        } else {
            chain.doFilter(request, response);
        }
    }
}
