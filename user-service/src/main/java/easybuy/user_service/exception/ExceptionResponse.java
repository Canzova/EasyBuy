package easybuy.user_service.exception;

import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
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

    @CreationTimestamp
    private LocalDateTime timestamp;

    private String path;
}
