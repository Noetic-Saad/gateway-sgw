package com.noeticworld.sgw.gateway.util;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "security")  //, configuration = CustomFeignConfig.class
public interface SecurityClient {

    @PostMapping("/oauth/validate")
    FeignResponse checkToken(@RequestBody SecurityRequest properties);
}
