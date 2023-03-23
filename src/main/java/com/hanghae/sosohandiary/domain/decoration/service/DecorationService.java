package com.hanghae.sosohandiary.domain.decoration.service;

import com.hanghae.sosohandiary.domain.decoration.dto.DecorationResponseDto;
import com.hanghae.sosohandiary.domain.decoration.entity.Decoration;
import com.hanghae.sosohandiary.domain.decoration.repository.DecorationRepository;
import com.hanghae.sosohandiary.domain.friend.dto.FriendResponseDto;
import com.hanghae.sosohandiary.domain.friend.entity.FriendRequest;
import com.hanghae.sosohandiary.domain.member.entity.Member;
import com.hanghae.sosohandiary.exception.ApiException;
import com.hanghae.sosohandiary.exception.ErrorHandling;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DecorationService {
    private final DecorationRepository decorationRepository;
    public Decoration saveDecoration(String imageURL) {
        Decoration decoration=decorationRepository.save(Decoration.of(imageURL));
        return decoration;
    }

    public List<DecorationResponseDto> getDecoration() {

        List<Decoration> decorationList = decorationRepository.findAll();
        List<DecorationResponseDto> decorationResponseDtoList = new ArrayList<>();

        for (Decoration decoration : decorationList) {
            decorationResponseDtoList.add(DecorationResponseDto.from(decoration.getId(),decoration.getImageURL()));
        }
        return decorationResponseDtoList;
    }
}
