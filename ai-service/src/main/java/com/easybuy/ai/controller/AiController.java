package com.easybuy.ai.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AiController {

    @GetMapping()
    public String sayHello(){
        return "Running inside EC2 Instance.";
    }

}
