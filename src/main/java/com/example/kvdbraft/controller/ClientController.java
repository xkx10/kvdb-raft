package com.example.kvdbraft.controller;

import com.example.kvdbraft.service.ClientOperationService;
import com.example.kvdbraft.vo.Result;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class ClientController {

    @Resource
    private ClientOperationService clientOperationService;

    @RequestMapping("/operate")
    @ResponseBody
    Result<String> operate(String command) {
        Object execute = clientOperationService.execute(command);
        if (execute instanceof String) {
            return Result.success((String) execute);
        } else {
            return (Boolean) execute ? Result.success("success") : Result.failure("false");
        }
    }
}
