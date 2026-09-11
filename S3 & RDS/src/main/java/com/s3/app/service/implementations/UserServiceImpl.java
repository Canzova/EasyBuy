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
import org.jspecify.annotations.NonNull;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final PostService postService;
    private final PostRepository postRepository;

    @Override
    public UserResultDTO createUser(UserRequestDTO userRequestDTO, MultipartFile profilePicture) {
        // Step 1 : Check if this user already exists
        if(userRepository.findById(userRequestDTO.getId()).isPresent()) throw new RuntimeException("User name already exists");

        // Step 2 : This user has a unique userName so now convert this into Entity
        User user = modelMapper.map(userRequestDTO, User.class);

        // Think about what will happen if the profilePicture is null or not send

        // Step 3 : Now you have to upload the image/profilePicture to s3
        if(profilePicture != null) {
            try {
                postService.uploadPost(user, List.of(profilePicture), true);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        UserResultDTO userResultDTO = modelMapper.map(userRepository.save(user), UserResultDTO.class);
        return setProfilePictureObjectUrl(user, userResultDTO);
    }

    private UserResultDTO setProfilePictureObjectUrl(User user, UserResultDTO userResultDTO) {
        // Not set the profile Image link before returning
        String profilePictureObjectKey =
                postRepository.findAllByUser(user).stream()
                        .filter(PostEntity::getIsProfilePicture)
                        .map(PostEntity::getObjectKey)
                        .findFirst()
                        .orElse(null);

        userResultDTO.setProfilePicture(profilePictureObjectKey);

        return userResultDTO;
    }
}
