package com.easybuy.common.exceptions;

import lombok.*;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ExceptionResponse {
    private String message;
    private Integer errorCode;
    private HttpStatus error;

    private LocalDateTime timestamp;

    private String path;
}
