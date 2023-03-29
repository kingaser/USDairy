package com.hanghae.sosohandiary.domain.decoration.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
public class DecorationResponseDto {

    private Long id;
    private String imageURL;

    @Builder
    private DecorationResponseDto(Long id, String imageURL) {
        this.id = id;
        this.imageURL = imageURL;
    }

    public static DecorationResponseDto of(Long id, String imageURL) {
        return DecorationResponseDto.builder()
                .id(id)
                .imageURL(imageURL)
                .build();
    }
}
