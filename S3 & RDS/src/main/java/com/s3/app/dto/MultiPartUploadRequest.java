package com.s3.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MultiPartUploadRequest {
    @NotBlank(message = "File name cannot be blank/null or empty")
    @Size(min = 1, max = 100)
    String fileName;

    @NotBlank(message = "User name cannot be blank/null or empty")
    @Size(min = 1, max = 100)
    String userName;
}
