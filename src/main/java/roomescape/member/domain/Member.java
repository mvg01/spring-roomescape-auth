package roomescape.member.domain;

public class Member {
    private final Long id;
    private final String loginId;
    private final String name;
    private final String password;
    private final Role role;

    private Member(Long id, String loginId, String name, String password, Role role) {
        this.id = id;
        this.loginId = loginId;
        this.name = name;
        this.password = password;
        this.role = role;
    }

    public static Member restore(Long id, String loginId, String name, String password, Role role) {
        return new Member(id, loginId, name, password, role);
    }

    public static Member of(String loginId, String name, String password, Role role) {
        if (loginId == null || loginId.isBlank()) {
            throw new IllegalArgumentException("아이디는 필수입니다.");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("이름은 필수입니다.");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("비밀번호는 필수입니다.");
        }
        if (role == null) {
            throw new IllegalArgumentException("권한은 필수입니다.");
        }
        return new Member(null, loginId, name, password, role);
    }

    public boolean matchesPassword(String rawPassword) {
        return password.equals(rawPassword);
    }

    public Long getId() {
        return id;
    }

    public String getLoginId() {
        return loginId;
    }

    public String getName() {
        return name;
    }

    public String getPassword() {
        return password;
    }

    public Role getRole() {
        return role;
    }
}
