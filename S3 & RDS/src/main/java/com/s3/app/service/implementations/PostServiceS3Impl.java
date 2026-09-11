package com.s3.app.service.implementations;

import com.s3.app.entity.PostEntity;
import com.s3.app.entity.User;
import com.s3.app.repository.PostRepository;
import com.s3.app.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PostServiceS3Impl implements PostService {

    private final S3Presigner s3Presigner;
    private final S3Client s3Client;
    private final PostRepository postRepository;

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
        if (files == null || files.isEmpty())
            throw new RuntimeException("Files list is null or empty");

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) continue;

            String contentType = file.getContentType();
            if (contentType == null || (!ALLOWED_TYPES_IMAGES.contains(contentType) && !ALLOWED_TYPES_VIDEOS.contains(contentType)))
                throw new RuntimeException("Invalid file type: " + contentType + ". Only images and videos are allowed");

            boolean isImage = ALLOWED_TYPES_IMAGES.contains(contentType);

            // Step 1 : Create the object key
            String objectKey = user.getUsername() + "/" + file.getOriginalFilename();

            // Step 2 : Build the PutObjectRequest
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .contentType(file.getContentType())
                    .bucket(awsBucketName)
                    .key(objectKey)
                    .build();

            // Step 3 : Upload the file — putObjectRequest has the bucket/key/contentType metadata, RequestBody.fromBytes() converts the file bytes as the actual content to upload
            PutObjectResponse putObjectResponse =
                    s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));

            // Step 4 : Save the post info into db
            savePostMetaData(user, objectKey, file, isProfilePic,isImage);
        }
    }

    @Override
    public void getPresignedImage(PostEntity post) {

    }

    private void savePostMetaData(User user, String objectKey, MultipartFile file, Boolean isProfilePic, Boolean isImage) {
        PostEntity postEntity = PostEntity.builder()
                .user(user)
                .objectKey(objectKey)
                .contentType(file.getContentType())
                .isImage(isImage)
                .isProfilePicture(isProfilePic)
                .build();
    }
}
