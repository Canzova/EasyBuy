package com.s3.app.service.implementations;

import com.s3.app.entity.PostEntity;
import com.s3.app.entity.User;
import com.s3.app.repository.PostRepository;
import com.s3.app.repository.UserRepository;
import com.s3.app.service.PostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostServiceS3Impl implements PostService {

    private final S3Presigner s3Presigner;
    private final S3Client s3Client;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    @Value("${AWS_S3_BUCKET_NAME}")
    private String awsBucketName;

    private static final Set<String> ALLOWED_TYPES_IMAGES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );

    private static final Set<String> ALLOWED_TYPES_VIDEOS = Set.of(
            "video/mp4", "video/mpeg", "video/quicktime", "video/webm"
    );

    @Override
    public void uploadPost(User user, List<MultipartFile> files, Boolean isProfilePic) throws IOException {
        log.info("uploadPost called for user: {}, isProfilePic: {}", user.getUsername(), isProfilePic);

        if (files == null || files.isEmpty()) {
            log.error("Files list is null or empty");
            throw new RuntimeException("Files list is null or empty");
        }

        log.info("Total files received: {}", files.size());

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                log.warn("Skipping null or empty file");
                continue;
            }

            String contentType = file.getContentType();
            log.info("Processing file: {}, contentType: {}, size: {} bytes", file.getOriginalFilename(), contentType, file.getSize());

            if (contentType == null || (!ALLOWED_TYPES_IMAGES.contains(contentType) && !ALLOWED_TYPES_VIDEOS.contains(contentType))) {
                log.error("Invalid file type: {}", contentType);
                throw new RuntimeException("Invalid file type: " + contentType + ". Only images and videos are allowed");
            }

            boolean isImage = ALLOWED_TYPES_IMAGES.contains(contentType);
            String objectKey = user.getUsername() + "/" + file.getOriginalFilename();
            log.info("Generated S3 object key: {}", objectKey);

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .contentType(file.getContentType())
                    .bucket(awsBucketName)
                    .key(objectKey)
                    .build();
            log.info("PutObjectRequest built for bucket: {}", awsBucketName);

            PutObjectResponse putObjectResponse =
                    s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));
            log.info("File uploaded to S3, ETag: {}", putObjectResponse.eTag());

            savePostMetaData(user, objectKey, file, isProfilePic, isImage);
        }

        log.info("uploadPost completed for user: {}", user.getUsername());
    }

    @Override
    public String getS3ObjectPresignedKey(String postKey) {
        log.info("getS3ObjectPresignedKey called for key: {}", postKey);

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(awsBucketName)
                .key(postKey)
                .build();

        GetObjectPresignRequest getObjectPresignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(5))
                .getObjectRequest(getObjectRequest)
                .build();

        PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(getObjectPresignRequest);
        String url = presigned.url().toString();
        log.info("Presigned URL generated: {}", url);

        return url;
    }


    private void savePostMetaData(User user, String objectKey, MultipartFile file, Boolean isProfilePic, Boolean isImage) {
        log.info("Saving post metadata - objectKey: {}, isImage: {}, isProfilePic: {}", objectKey, isImage, isProfilePic);
        PostEntity postEntity = PostEntity.builder()
                .user(user)
                .objectKey(objectKey)
                .contentType(file.getContentType())
                .isImage(isImage)
                .isProfilePicture(isProfilePic)
                .build();

        postRepository.save(postEntity);
        userRepository.save(user);
        log.info("PostEntity saved with id: {}", postEntity.getId());
    }
}
