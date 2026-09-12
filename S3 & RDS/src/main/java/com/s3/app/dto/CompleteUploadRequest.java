package com.s3.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CompleteUploadRequest {

    @NotBlank(message = "Key cannot be blank/null or empty")
    private String key;

    @NotBlank(message = "Upload ID cannot be blank/null or empty")
    private String uploadId;

    @NotEmpty(message = "Completed parts cannot be empty")
    private List<PartDTO> completedParts;
}
