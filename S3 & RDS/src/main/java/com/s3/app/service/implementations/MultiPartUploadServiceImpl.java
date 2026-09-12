package com.s3.app.service.implementations;

import com.s3.app.dto.MultiPartUploadRequest;
import com.s3.app.dto.MultiPartUploadResponse;
import com.s3.app.service.MultiPartUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.UploadPartPresignRequest;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MultiPartUploadServiceImpl implements MultiPartUploadService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${AWS_S3_BUCKET_NAME}")
    private String bucketName;

    @Override
    public MultiPartUploadResponse initiateUpload(MultiPartUploadRequest multiPartUploadRequest) {
        String objectKey = multiPartUploadRequest.getUserName() + "/" + multiPartUploadRequest.getFileName() + UUID.randomUUID().toString();
        log.info("[INITIATE] user={} file={} key={}", multiPartUploadRequest.getUserName(), multiPartUploadRequest.getFileName(), objectKey);

        CreateMultipartUploadRequest multipartUploadRequest = CreateMultipartUploadRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();

        CreateMultipartUploadResponse multiPartUpload = s3Client.createMultipartUpload(multipartUploadRequest);
        log.info("[INITIATE] uploadId={}", multiPartUpload.uploadId());

        return new MultiPartUploadResponse(objectKey, multiPartUpload.uploadId());
    }

    @Override
    public String generatePartUploadUrl(String key, String uploadId, int partNumber) {
        log.info("[PRESIGN] partNumber={} uploadId={} key={}", partNumber, uploadId, key);

        UploadPartRequest uploadPartRequest = UploadPartRequest.builder()
                .bucket(bucketName)
                .key(key)
                .uploadId(uploadId)
                .partNumber(partNumber)
                .build();

        UploadPartPresignRequest uploadPartPresignRequest = UploadPartPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10))
                .uploadPartRequest(uploadPartRequest)
                .build();

        String url = s3Presigner.presignUploadPart(uploadPartPresignRequest).url().toString();
        log.info("[PRESIGN] generated URL for part {}", partNumber);
        return url;
    }

    @Override
    public void completeUpload(String key, String uploadId, List<CompletedPart> completedParts) {
        log.info("[COMPLETE] key={} uploadId={} totalParts={}", key, uploadId, completedParts.size());
        completedParts.forEach(p -> log.debug("[COMPLETE] part={} eTag={}", p.partNumber(), p.eTag()));

        CompleteMultipartUploadRequest completeMultipartUploadRequest = CompleteMultipartUploadRequest.builder()
                .bucket(bucketName)
                .key(key)
                .uploadId(uploadId)
                .multipartUpload(CompletedMultipartUpload.builder().parts(completedParts).build())
                .build();

        s3Client.completeMultipartUpload(completeMultipartUploadRequest);
        log.info("[COMPLETE] successfully assembled object key={}", key);
    }

    @Override
    public List<Part> listUploadedParts(String key, String uploadId) {
        log.info("[LIST_PARTS] key={} uploadId={}", key, uploadId);
        ListPartsRequest listPartsRequest = ListPartsRequest.builder()
                .bucket(bucketName)
                .key(key)
                .uploadId(uploadId)
                .build();

        List<Part> parts = s3Client.listParts(listPartsRequest).parts();
        log.info("[LIST_PARTS] found {} parts", parts.size());
        return parts;
    }

    @Override
    public void abortUpload(String key, String uploadId) {
        log.warn("[ABORT] key={} uploadId={}", key, uploadId);
        AbortMultipartUploadRequest abortMultipartUploadRequest = AbortMultipartUploadRequest.builder()
                .bucket(bucketName)
                .key(key)
                .uploadId(uploadId)
                .build();

        s3Client.abortMultipartUpload(abortMultipartUploadRequest);
        log.warn("[ABORT] upload aborted successfully");
    }
}
