package com.s3.app.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UserResultDTO {
    private Long id;
    private String username;
    private String name;
    private String bio;
    private String profilePicture;
}
