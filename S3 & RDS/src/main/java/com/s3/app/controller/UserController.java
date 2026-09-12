package com.s3.app.controller;

import com.s3.app.dto.UserRequestDTO;
import com.s3.app.dto.UserResultDTO;
import com.s3.app.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping(value = "/create", consumes =  MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserResultDTO> createUser(@Valid @RequestPart UserRequestDTO userRequestDTO, @RequestPart(name = "images")MultipartFile images){
        UserResultDTO userResultDTO = userService.createUser(userRequestDTO, images);
        return new ResponseEntity<>(userResultDTO, HttpStatus.CREATED);
    }

    @GetMapping("/get/image")
    public ResponseEntity<List<String>> getUserImage(@Valid @RequestBody List<String> postObjectKey){
        List<String> s3ObjectKeyPresigned = userService.getUserImage(postObjectKey);
        return new ResponseEntity<>(s3ObjectKeyPresigned, HttpStatus.OK);
    }

}
