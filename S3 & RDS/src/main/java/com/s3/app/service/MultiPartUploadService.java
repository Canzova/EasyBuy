package com.s3.app.service;

import com.s3.app.dto.MultiPartUploadRequest;
import com.s3.app.dto.MultiPartUploadResponse;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.Part;

import java.util.List;

public interface MultiPartUploadService {

    // Method 1 : To initiate the multipart upload
    MultiPartUploadResponse initiateUpload(MultiPartUploadRequest multiPartUploadRequest);

    // Metho 2 : To get the link for each upload part
    String generatePartUploadUrl(String key, String uploadId, int partNumber);

    // Method 3 : To combine all the uploaded parts on S3
    void completeUpload(String key, String uploadId, List<CompletedPart> completedParts);

    // Method 4 : Called by frontend after disconnection : To get no of parts which are successfully uploaded on S3
    List<Part> listUploadedParts(String key,  String uploadId);

    // Method 5 : Abort upload
    void abortUpload(String key, String uploadId);
}
