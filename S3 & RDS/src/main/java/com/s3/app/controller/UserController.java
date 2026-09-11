package com.s3.app.controller;

import com.s3.app.dto.UserRequestDTO;
import com.s3.app.dto.UserResultDTO;
import com.s3.app.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/create")
    public ResponseEntity<UserResultDTO> createUser(@RequestParam UserRequestDTO userRequestDTO, @RequestParam(name = "images")MultipartFile images){
        UserResultDTO userResultDTO = userService.createUser(userRequestDTO, images);
        return new ResponseEntity<>(userResultDTO, HttpStatus.CREATED);
    }


}
