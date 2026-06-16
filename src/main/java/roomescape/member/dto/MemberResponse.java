package roomescape.member.dto;

import roomescape.member.domain.Member;

public record MemberResponse(Long id, String loginId, String name, String role) {

    public static MemberResponse from(Member member) {
        return new MemberResponse(member.getId(), member.getLoginId(), member.getName(), member.getRole().toString());
    }
}
