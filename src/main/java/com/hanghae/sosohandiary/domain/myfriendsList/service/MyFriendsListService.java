package com.hanghae.sosohandiary.domain.myfriendsList.service;

import com.hanghae.sosohandiary.domain.member.entity.Member;
import com.hanghae.sosohandiary.domain.member.repository.MemberRepository;
import com.hanghae.sosohandiary.domain.myfriendsList.dto.FriendListResponseDto;
import com.hanghae.sosohandiary.domain.myfriendsList.dto.MyFriendResponseDto;
import com.hanghae.sosohandiary.domain.myfriendsList.entity.FriendRequest;
import com.hanghae.sosohandiary.domain.myfriendsList.entity.MyFriendsList;
import com.hanghae.sosohandiary.domain.myfriendsList.repository.FriendRequestRepository;
import com.hanghae.sosohandiary.domain.myfriendsList.repository.MyFriendsListRepository;
import com.hanghae.sosohandiary.exception.ApiException;
import com.hanghae.sosohandiary.exception.ErrorHandling;
import com.hanghae.sosohandiary.security.MemberDetailsImpl;
import com.hanghae.sosohandiary.utils.MessageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyFriendsListService {
    private final MyFriendsListRepository myFriendsListRepository;
    private final FriendRequestRepository friendRequestRepository;
    private final MemberRepository memberRepository;


    @Transactional
    public MessageDto createFriendRequest(Long id, Member member) {
        if (member.getId().equals(id)) {
            return new MessageDto("자기 자신을 추가할 수 없습니다.", HttpStatus.BAD_REQUEST);
        }

        Optional<Member> userId = memberRepository.findById(member.getId());
        Optional<Member> friendId = memberRepository.findById(id);

        Optional<FriendRequest> findUserId = friendRequestRepository.findById(userId.get().getId());


        // 친구 요청 중복 체크
        boolean isDuplicated = friendRequestRepository.existsByFriend_IdAndMember_Id(id, userId.get().getId());
        if (isDuplicated) {
            return new MessageDto("이미 친구 요청이 존재합니다.", HttpStatus.BAD_REQUEST);
        }

        FriendRequest friendRequest = FriendRequest.builder()
                .member(userId.get())
                .friend(friendId.get())
                .build();

        friendRequestRepository.save(friendRequest);
        return new MessageDto("친구요청 성공", HttpStatus.CREATED);
    }

    public List<MyFriendResponseDto> getFriendRequest(Member member) {
        List<FriendRequest> friendRequestList = friendRequestRepository.findAllByFriendIdOrderByCreatedAtDesc(member.getId());
        List<MyFriendResponseDto> myFriendResponseDtoList = new ArrayList<>();

        for (FriendRequest friendRequest : friendRequestList) {
            myFriendResponseDtoList.add(MyFriendResponseDto.from(friendRequest.getId(),friendRequest.getMember().getEmail()));
        }
        return myFriendResponseDtoList;
    }

    @Transactional
    public MessageDto createFriendAccept(Long id, Member member) {
        Optional<FriendRequest> addId = friendRequestRepository.findById(id);

        if (!addId.get().getFriend().getId().equals(member.getId())) {
            throw new ApiException(ErrorHandling.NOT_MATCH_AUTHORIZATION);
        }

        MyFriendsList myFriendsList = MyFriendsList.builder()
                .member(addId.get().getMember())
                .friend(addId.get().getFriend())
                .build();


        MyFriendsList myFriendsList2 = MyFriendsList.builder()
                .member(addId.get().getFriend())
                .friend(addId.get().getMember())
                .build();

        myFriendsListRepository.save(myFriendsList);
        myFriendsListRepository.save(myFriendsList2);
        friendRequestRepository.deleteById(id);
        return new MessageDto("친구추가 성공", HttpStatus.ACCEPTED);
    }


    public List<FriendListResponseDto> getFriendList(Member member) {
        List<MyFriendsList> myFriendsLists = myFriendsListRepository.findAllByMemberId(member.getId());
        List<FriendListResponseDto> friendListResponseDtoList = new ArrayList<>();

        for (MyFriendsList myFriendsList : myFriendsLists) {
            friendListResponseDtoList.add(FriendListResponseDto.from(myFriendsList.getId(),myFriendsList.getFriend().getEmail()));
        }
        return friendListResponseDtoList;
    }

    @Transactional
    public MessageDto deleteFriendList(Long id, Member member) {
        MyFriendsList myFriendsList = myFriendsListRepository.findById(id).orElseThrow(
                () -> new ApiException(ErrorHandling.NOT_FOUND_USER)
        );

        if (!myFriendsList.getMember().getId().equals(member.getId())) {
            throw new ApiException(ErrorHandling.NOT_MATCH_AUTHORIZATION);
        }

        myFriendsListRepository.deleteByMemberIdAndFriendId(myFriendsList.getMember().getId(), myFriendsList.getFriend().getId());
        myFriendsListRepository.deleteByFriendIdAndMemberId(myFriendsList.getMember().getId(), myFriendsList.getFriend().getId());
        return MessageDto.of("친구 삭제 완료", HttpStatus.OK);
    }
}
