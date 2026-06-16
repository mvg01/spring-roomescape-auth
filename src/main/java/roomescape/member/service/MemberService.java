package roomescape.member.service;

import org.springframework.stereotype.Service;
import roomescape.common.exception.BusinessException;
import roomescape.common.exception.ErrorCode;
import roomescape.member.domain.Member;
import roomescape.member.domain.Role;
import roomescape.member.dto.MemberRequest;
import roomescape.member.dto.MemberResponse;
import roomescape.member.repository.MemberRepository;

@Service
public class MemberService {

    private final MemberRepository memberRepository;

    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    public MemberResponse createMember(MemberRequest memberRequest) {
        if (memberRepository.findByLoginId(memberRequest.loginId()).isPresent()) {
            throw new BusinessException(ErrorCode.MEMBER_ALREADY_EXISTS);
        }
        Member member = Member.of(memberRequest.loginId(), memberRequest.name(), memberRequest.password(), Role.USER);
        return MemberResponse.from(memberRepository.save(member));
    }
}
