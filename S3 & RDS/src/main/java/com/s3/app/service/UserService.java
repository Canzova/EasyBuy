package com.s3.app.service;

import com.s3.app.dto.UserRequestDTO;
import com.s3.app.dto.UserResultDTO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface UserService {
    UserResultDTO createUser(UserRequestDTO userRequestDTO, MultipartFile images);

    List<String> getUserImage(List<String> postObjectKey);
}
