package com.s3.app.controller;

import com.s3.app.dto.CompleteUploadRequest;
import com.s3.app.dto.MultiPartUploadRequest;
import com.s3.app.dto.MultiPartUploadResponse;
import com.s3.app.service.MultiPartUploadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.Part;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/multipart")
@RequiredArgsConstructor
public class MultiPartUploadController {

    private final MultiPartUploadService multiPartUploadService;

    @PostMapping("/initiate")
    public ResponseEntity<MultiPartUploadResponse> initiateUpload(@Valid @RequestBody MultiPartUploadRequest request) {
        log.info("[REQ] POST /initiate user={} file={}", request.getUserName(), request.getFileName());
        MultiPartUploadResponse response = multiPartUploadService.initiateUpload(request);
        log.info("[RES] POST /initiate key={} uploadId={}", response.getKey(), response.getUploadId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/presign")
    public ResponseEntity<String> generatePartUploadUrl(
            @RequestParam String key,
            @RequestParam String uploadId,
            @RequestParam int partNumber) {
        log.info("[REQ] GET /presign partNumber={}", partNumber);
        return ResponseEntity.ok(multiPartUploadService.generatePartUploadUrl(key, uploadId, partNumber));
    }

    @PostMapping("/complete")
    public ResponseEntity<Void> completeUpload(@Valid @RequestBody CompleteUploadRequest request) {
        log.info("[REQ] POST /complete key={} parts={}", request.getKey(), request.getCompletedParts().size());
        List<CompletedPart> parts = request.getCompletedParts().stream()
                .map(p -> CompletedPart.builder()
                        .partNumber(p.getPartNumber())
                        .eTag(p.getETag())
                        .build())
                .toList();
        multiPartUploadService.completeUpload(request.getKey(), request.getUploadId(), parts);
        log.info("[RES] POST /complete success");
        return ResponseEntity.ok().build();
    }

    @GetMapping("/parts")
    public ResponseEntity<List<Part>> listUploadedParts(
            @RequestParam String key,
            @RequestParam String uploadId) {
        return ResponseEntity.ok(multiPartUploadService.listUploadedParts(key, uploadId));
    }

    @DeleteMapping("/abort")
    public ResponseEntity<Void> abortUpload(
            @RequestParam String key,
            @RequestParam String uploadId) {
        log.warn("[REQ] DELETE /abort key={} uploadId={}", key, uploadId);
        multiPartUploadService.abortUpload(key, uploadId);
        return ResponseEntity.ok().build();
    }
}
