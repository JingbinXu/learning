package com.bing.bingaicode.controller;

import com.bing.bingaicode.common.BaseResponse;
import com.bing.bingaicode.common.ResultUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/")
    public BaseResponse<String> heathCheck() {
        return ResultUtils.success("ok!");
    }
}
