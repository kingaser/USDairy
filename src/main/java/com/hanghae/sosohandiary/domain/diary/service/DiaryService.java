package com.hanghae.sosohandiary.domain.diary.service;

import com.hanghae.sosohandiary.domain.diary.dto.DiaryRequestDto;
import com.hanghae.sosohandiary.domain.diary.dto.DiaryResponseDto;
import com.hanghae.sosohandiary.domain.diary.entity.Diary;
import com.hanghae.sosohandiary.domain.diary.repository.DiaryRepository;
import com.hanghae.sosohandiary.domain.diarydetil.entity.DiaryDetail;
import com.hanghae.sosohandiary.domain.diarydetil.repository.DiaryDetailRepository;
import com.hanghae.sosohandiary.domain.member.entity.Member;
import com.hanghae.sosohandiary.exception.ApiException;
import com.hanghae.sosohandiary.exception.ErrorHandling;
import com.hanghae.sosohandiary.utils.MessageDto;
import com.hanghae.sosohandiary.utils.s3.S3Service;
import lombok.RequiredArgsConstructor;
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
                return DiaryResponseDto.from(diary, null, member);
            }
        }
        if (multipartFileList != null) {
            s3Service.uploadDiary(multipartFileList, diaryRequestDto, member);
        }

        String uploadImageUrl = s3Service.getUploadImageUrl();

        Diary diary = diaryRepository.save(Diary.of(diaryRequestDto, uploadImageUrl, member));

        return DiaryResponseDto.from(diary, uploadImageUrl, member);
    }

    public List<DiaryResponseDto> findDiaryList() {
        List<Diary> diaryList = diaryRepository.findAllByOrderByModifiedAtDesc().orElseThrow(
                () -> new ApiException(ErrorHandling.NOT_FOUND_DIARY)
        );
        List<DiaryResponseDto> diaryResponseDtoList = new ArrayList<>();

        for (Diary diary : diaryList) {
            String uploadImageUrl = diary.getUploadPath();
            diaryResponseDtoList.add(DiaryResponseDto.from(diary, uploadImageUrl, diary.getMember()));
        }

        return diaryResponseDtoList;
    }

    @Transactional
    public DiaryResponseDto modifyDiary(Long id, DiaryRequestDto diaryRequestDto, List<MultipartFile> multipartFileList, Member member) throws IOException {
        Diary diary = diaryRepository.findById(id).orElseThrow(
                () -> new ApiException(ErrorHandling.NOT_FOUND_DIARY)
        );

        if (!diary.getMember().getId().equals(member.getId())) {
            throw new ApiException(ErrorHandling.NOT_MATCH_AUTHORIZATION);
        }

        if (diary.getUploadPath() != null) {
            String uploadPath = diary.getUploadPath();
            String filename = uploadPath.substring(50);
            s3Service.deleteFile(filename);
        }

        for (MultipartFile multipartFile : multipartFileList) {
            if (multipartFile.getOriginalFilename().equals("")) {
                String uploadImageUrl = diary.getUploadPath();
                diary.update(diaryRequestDto, uploadImageUrl);
                return DiaryResponseDto.from(diary, uploadImageUrl, member);
            }
        }

        if (multipartFileList != null) {
            s3Service.uploadDiary(multipartFileList, diaryRequestDto, member);
        }
        String uploadImageUrl = s3Service.getUploadImageUrl();

        diary.update(diaryRequestDto, uploadImageUrl);

        return DiaryResponseDto.from(diary, uploadImageUrl, member);
    }

    @Transactional
    public MessageDto removeDiary(Long id, Member member) {

        Diary diary = diaryRepository.findById(id).orElseThrow(
                () -> new ApiException(ErrorHandling.NOT_FOUND_DIARY)
        );

        if (!diary.getMember().getId().equals(member.getId())) {
            throw new ApiException(ErrorHandling.NOT_MATCH_AUTHORIZATION);
        }

        if (diary.getUploadPath() != null) {
            String uploadPath = diary.getUploadPath();
            String filename = uploadPath.substring(50);
            s3Service.deleteFile(filename);
        }

        List<DiaryDetail> diaryDetailList = diaryDetailRepository.findAllByDiaryId(id).orElseThrow(
                () -> new ApiException(ErrorHandling.NOT_FOUND_DIARY)
        );


        for (DiaryDetail diaryDetail : diaryDetailList) {
            if (diaryDetail.getDetailUploadPath() != null) {
                String detailUploadPath = diaryDetail.getDetailUploadPath();
                String detailFilename = detailUploadPath.substring(50);
                s3Service.deleteFile(detailFilename);
            }
        }

        diaryDetailRepository.deleteAllByDiaryId(id);
        diaryRepository.deleteById(id);

        return MessageDto.of("다이어리 삭제 완료", HttpStatus.OK);
    }
}
