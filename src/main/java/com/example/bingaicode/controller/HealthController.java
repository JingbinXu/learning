package com.example.bingaicode.controller;

import com.example.bingaicode.common.BaseResponse;
import com.example.bingaicode.common.ResultUtils;
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
