package com.hanghae.sosohandiary.domain.member.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.hanghae.sosohandiary.domain.member.dto.JoinRequestDto;
import com.hanghae.sosohandiary.domain.member.dto.LoginRequestDto;
import com.hanghae.sosohandiary.domain.member.service.KakaoMemberService;
import com.hanghae.sosohandiary.domain.member.service.MemberService;
import com.hanghae.sosohandiary.utils.MessageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

@RestController
@RequiredArgsConstructor
@RequestMapping("/")
public class MemberController {

    private final KakaoMemberService kakaoMemberService;
    private final MemberService memberService;

    @GetMapping("/login/kakao")
    public MessageDto kakaoLogin(@RequestParam String code, HttpServletResponse response) throws JsonProcessingException {
        return kakaoMemberService.kakaoLogin(code, response);
    }

    @PostMapping("/join")
    public MessageDto join(@Valid @RequestBody JoinRequestDto joinRequestDto) {
        return memberService.join(joinRequestDto);
    }

    @PostMapping("/login")
    public MessageDto login(@RequestBody LoginRequestDto loginRequestDto, HttpServletResponse response) {
        return memberService.login(loginRequestDto, response);
    }



}
