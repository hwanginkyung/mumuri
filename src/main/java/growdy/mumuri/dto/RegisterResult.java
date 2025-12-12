package growdy.mumuri.dto;

import growdy.mumuri.domain.Member;

public record RegisterResult(
        Member member,
        boolean isNew
) {}
