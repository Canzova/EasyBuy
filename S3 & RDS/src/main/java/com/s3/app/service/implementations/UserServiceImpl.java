package com.s3.app.service.implementations;

import com.s3.app.dto.UserRequestDTO;
import com.s3.app.dto.UserResultDTO;
import com.s3.app.entity.PostEntity;
import com.s3.app.entity.User;
import com.s3.app.repository.PostRepository;
import com.s3.app.repository.UserRepository;
import com.s3.app.service.PostService;
import com.s3.app.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final PostService postService;
    private final PostRepository postRepository;

    @Override
    public UserResultDTO createUser(UserRequestDTO userRequestDTO, MultipartFile profilePicture) {
        log.info("createUser called for username: {}", userRequestDTO.getUsername());

        if(userRepository.findByUsername(userRequestDTO.getUsername()) != null) {
            log.info("Username already exists: {}", userRequestDTO.getUsername());
            throw new RuntimeException("User name already exists");
        }

        User user = modelMapper.map(userRequestDTO, User.class);
        log.info("Mapped UserRequestDTO to User entity");

        user = userRepository.save(user);
        log.info("User saved to DB with username: {}", user.getUsername());

        if(profilePicture != null) {
            log.info("Profile picture provided, uploading to S3");
            try {
                postService.uploadPost(user, List.of(profilePicture), true);
            } catch (IOException e) {
                log.error("Failed to upload profile picture for user: {}", userRequestDTO.getUsername(), e);
                throw new RuntimeException("Could not upload profile picture" + e);
            }
        } else {
            log.info("No profile picture provided, skipping S3 upload");
        }

        UserResultDTO userResultDTO = modelMapper.map(user, UserResultDTO.class);

        return setProfilePictureObjectUrl(user, userResultDTO);
    }

    @Override
    public List<String> getUserImage(List<String> postObjectKey) {
        log.info("getUserImage called with {} keys", postObjectKey == null ? 0 : postObjectKey.size());

        if(postObjectKey == null || postObjectKey.isEmpty()) {
            log.warn("postObjectKey list is null or empty");
            throw new RuntimeException("Images object key cannot be empty");
        }

        List<String> s3ObjectKeyPresigned = new ArrayList<>();
        for(String postKey : postObjectKey) {
            log.info("Generating presigned URL for key: {}", postKey);
            s3ObjectKeyPresigned.add(postService.getS3ObjectPresignedKey(postKey));
        }

        log.info("Returning {} presigned URLs", s3ObjectKeyPresigned.size());
        return s3ObjectKeyPresigned;
    }

    private UserResultDTO setProfilePictureObjectUrl(User user, UserResultDTO userResultDTO) {
        log.info("Fetching profile picture object key for user: {}", user.getUsername());
        String profilePictureObjectKey =
                postRepository.findAllByUser(user).stream()
                        .filter(PostEntity::getIsProfilePicture)
                        .map(PostEntity::getObjectKey)
                        .findFirst()
                        .orElse(null);

        log.info("Profile picture object key: {}", profilePictureObjectKey);
        userResultDTO.setProfilePicture(profilePictureObjectKey);
        return userResultDTO;
    }
}
