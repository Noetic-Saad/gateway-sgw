package com.noeticworld.sgw.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.noeticworld.sgw.gateway.util.FeignResponse;
import com.noeticworld.sgw.gateway.util.GatewayResponse;
import com.noeticworld.sgw.gateway.util.SecurityClient;
import com.noeticworld.sgw.gateway.util.SecurityRequest;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import feign.FeignException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

public class RequestFilter extends OncePerRequestFilter {

    @Autowired
    SecurityClient securityClient;

    public RequestFilter() {
    }

    @Override
    protected void doFilterInternal(HttpServletRequest httpServletRequest,
                                    HttpServletResponse httpServletResponse,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        ObjectMapper objectMapper = new ObjectMapper();
        FeignResponse response = null;
        Map<String, String> res;
        if (httpServletRequest.getRequestURI().equals("/sgw/authorize") || httpServletRequest.getRequestURI().equals("/sgw/user/mo")) {
            filterChain.doFilter(httpServletRequest, httpServletResponse);
        } else {
            String token = httpServletRequest.getHeader("Authorization");
            OkHttpClient client = new OkHttpClient();
            SecurityRequest request = new SecurityRequest();
            if (token == null) {
                httpServletResponse.sendError(400, "Token Not Found");
                return;
            }
            String reqToken;
            try{
                reqToken = token.substring(7);
            }catch (StringIndexOutOfBoundsException e){
                System.out.println("THIS IS THE TOKEN : "+token);
                httpServletResponse.sendError(403, "Invalid Token or Token Is Expired");
                return;
            }
            request.setToken(reqToken);
            try {
                response = securityClient.checkToken(request);
            } catch (FeignException e) {
                GatewayResponse response1 = new GatewayResponse();
                httpServletResponse.sendError(403, "Invalid Token or Token Is Expired");
                return;
            }

            if (response != null) {
                MyHttpServletRequestWrapper httpServletRequestWrapper = new MyHttpServletRequestWrapper((HttpServletRequest) httpServletRequest);
                httpServletRequestWrapper.addHeader("vendorPlanId", response.getVendorPlanId());
                try {
                    filterChain.doFilter((HttpServletRequest) httpServletRequestWrapper, httpServletResponse);
                } catch (ServletException e) {
                }

            }
        }
    }
}

class MyHttpServletRequestWrapper extends HttpServletRequestWrapper {
    public MyHttpServletRequestWrapper(HttpServletRequest request) {
        super(request);
    }

    private Map<String, String> headerMap = new HashMap<String, String>();

    public void addHeader(String name, String value) {
        headerMap.put(name, value);
    }

    public void removeHeader(String name) {
        headerMap.remove(name);
    }

    @Override
    public String getHeader(String name) {
        String headerValue = super.getHeader(name);
        if (headerMap.containsKey(name)) {
            headerValue = headerMap.get(name);
        }
        return headerValue;
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        List<String> names = Collections.list(super.getHeaderNames());
        for (String name : headerMap.keySet()) {
            names.add(name);
        }
        return Collections.enumeration(names);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        List<String> values = Collections.list(super.getHeaders(name));
        if (headerMap.containsKey(name)) {
            values.add(headerMap.get(name));
        }
        return Collections.enumeration(values);
    }

}

