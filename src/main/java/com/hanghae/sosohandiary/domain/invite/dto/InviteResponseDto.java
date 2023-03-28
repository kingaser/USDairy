package com.hanghae.sosohandiary.domain.invite.dto;

import com.hanghae.sosohandiary.domain.invite.entity.Invite;
import com.hanghae.sosohandiary.domain.member.entity.Member;
import lombok.Builder;
import lombok.Getter;

@Getter
public class InviteResponseDto {

    private Long inviteId;
    private String nickname;

    @Builder
    private InviteResponseDto(Invite invite, Member member) {
        inviteId = invite.getId();
        nickname = member.getNickname();
    }

    public static InviteResponseDto from(Invite invite, Member member) {
        return InviteResponseDto.builder()
                .invite(invite)
                .member(member)
                .build();
    }

}
