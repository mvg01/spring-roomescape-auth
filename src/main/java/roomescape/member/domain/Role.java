package roomescape.member.domain;

public enum Role {
    USER,
    ADMIN;

    public static Role of(String role) {
        return  Role.valueOf(role);
    }
}
