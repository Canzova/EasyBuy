package com.s3.app.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PartDTO {
    private int partNumber;
    private String eTag;
}
