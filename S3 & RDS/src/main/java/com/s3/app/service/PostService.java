package com.s3.app.service;

import com.s3.app.entity.PostEntity;
import com.s3.app.entity.User;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface PostService {
    void uploadPost(User user, List<MultipartFile> profilePicture, Boolean isProfilePic) throws IOException;

    String getS3ObjectPresignedKey(String postKey);
}
