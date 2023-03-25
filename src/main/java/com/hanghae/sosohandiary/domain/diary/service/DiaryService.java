package com.hanghae.sosohandiary.domain.diary.service;

import com.hanghae.sosohandiary.domain.diary.dto.DiaryRequestDto;
import com.hanghae.sosohandiary.domain.diary.dto.DiaryResponseDto;
import com.hanghae.sosohandiary.domain.diary.entity.Diary;
import com.hanghae.sosohandiary.domain.diary.entity.DiaryCondition;
import com.hanghae.sosohandiary.domain.diary.repository.DiaryRepository;
import com.hanghae.sosohandiary.domain.diarydetail.repository.DiaryDetailRepository;
import com.hanghae.sosohandiary.domain.member.entity.Member;
import com.hanghae.sosohandiary.exception.ApiException;
import com.hanghae.sosohandiary.exception.ErrorHandling;
import com.hanghae.sosohandiary.utils.MessageDto;
import com.hanghae.sosohandiary.utils.page.PageCustom;
import com.hanghae.sosohandiary.utils.s3.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiaryService {

    private final DiaryDetailRepository diaryDetailRepository;
    private final DiaryRepository diaryRepository;
    private final S3Service s3Service;

    @Transactional
    public DiaryResponseDto saveDiary(DiaryRequestDto diaryRequestDto,
                                      List<MultipartFile> multipartFileList,
                                      Member member) throws IOException {
        for (MultipartFile multipartFile : multipartFileList) {
            if (multipartFile.getOriginalFilename().equals("")) {
                Diary diary = diaryRepository.save(Diary.of(diaryRequestDto, null, member));
                return DiaryResponseDto.from(diary, member);
            }
        }
        if (multipartFileList != null) {
            s3Service.uploadDiary(multipartFileList, diaryRequestDto, member);
        }

        String uploadImageUrl = s3Service.getUploadImageUrl();

        DiaryCondition condition = DiaryCondition.PUBLIC;

        if(!diaryRequestDto.getDiaryCondition().equals(condition)) {
            condition = DiaryCondition.PRIVATE;
        }

        Diary diary = diaryRepository.save(Diary.of(diaryRequestDto, uploadImageUrl, member));

        return DiaryResponseDto.from(diary, member);
    }

    public List<DiaryResponseDto> findDiaryList(Pageable pageable, Member member) {

        Page<Diary> diaryPage = diaryRepository.findAllByOrderByModifiedAtDesc(pageable);
        List<DiaryResponseDto> diaryResponseDtoList = new ArrayList<>();
        for (Diary diary : diaryPage) {
            if (diary.getDiaryCondition().equals(DiaryCondition.PUBLIC)) {
                diaryResponseDtoList.add(DiaryResponseDto.from(diary, diary.getMember()));
            } else if (diary.getMember().getId().equals(member.getId())) {
                diaryResponseDtoList.add(DiaryResponseDto.from(diary, diary.getMember()));
            }
        }

        return diaryResponseDtoList;
    }

    public PageCustom<DiaryResponseDto> findPublicDiaryList(Pageable pageable) {

        Page<DiaryResponseDto> diaryResponseDtoPagePublic = diaryRepository
                .findAllByDiaryConditionOrderByModifiedAtDesc(pageable, DiaryCondition.PUBLIC)
                .map((Diary diary) -> new DiaryResponseDto(diary, diary.getMember()));

        return new PageCustom<>(diaryResponseDtoPagePublic.getContent(),
                diaryResponseDtoPagePublic.getPageable(),
                diaryResponseDtoPagePublic.getTotalElements());
    }

    public PageCustom<DiaryResponseDto> findPrivateDiaryList(Pageable pageable,
                                                             Member member) {
        Page<DiaryResponseDto> diaryResponseDtoPagePrivate = diaryRepository
                .findAllByMemberIdAndDiaryConditionOrderByModifiedAtDesc(pageable, member.getId(), DiaryCondition.PRIVATE)
                .map((Diary diary) -> new DiaryResponseDto(diary, diary.getMember()));

        return new PageCustom<>(diaryResponseDtoPagePrivate.getContent(),
                diaryResponseDtoPagePrivate.getPageable(),
                diaryResponseDtoPagePrivate.getTotalElements());
    }

    @Transactional
    public DiaryResponseDto modifyDiary(Long id, DiaryRequestDto diaryRequestDto,
                                        List<MultipartFile> multipartFileList,
                                        Member member) throws IOException {
        Diary diary = diaryRepository.findById(id).orElseThrow(
                () -> new ApiException(ErrorHandling.NOT_FOUND_DIARY)
        );

        if (!diary.getMember().getId().equals(member.getId())) {
            throw new ApiException(ErrorHandling.NOT_MATCH_AUTHORIZATION);
        }

        if (diary.getImg() != null) {
            String uploadPath = diary.getImg();
            String filename = uploadPath.substring(50);
            s3Service.deleteFile(filename);
        }

        for (MultipartFile multipartFile : multipartFileList) {
            if (multipartFile.getOriginalFilename().equals("")) {
                String uploadImageUrl = diary.getImg();
                diary.update(diaryRequestDto, uploadImageUrl);
                return DiaryResponseDto.from(diary, member);
            }
        }

        if (multipartFileList != null) {
            s3Service.uploadDiary(multipartFileList, diaryRequestDto, member);
        }
        String uploadImageUrl = s3Service.getUploadImageUrl();

        diary.update(diaryRequestDto, uploadImageUrl);

        return DiaryResponseDto.from(diary, member);
    }

    @Transactional
    public MessageDto removeDiary(Long id, Member member) {

        Diary diary = diaryRepository.findById(id).orElseThrow(
                () -> new ApiException(ErrorHandling.NOT_FOUND_DIARY)
        );

        if (!diary.getMember().getId().equals(member.getId())) {
            throw new ApiException(ErrorHandling.NOT_MATCH_AUTHORIZATION);
        }

        if (diary.getImg() != null) {
            String uploadPath = diary.getImg();
            String filename = uploadPath.substring(50);
            s3Service.deleteFile(filename);
        }

        diaryDetailRepository.deleteAllByDiaryId(id);
        diaryRepository.deleteById(id);

        return MessageDto.of("다이어리 삭제 완료", HttpStatus.OK);
    }

}
