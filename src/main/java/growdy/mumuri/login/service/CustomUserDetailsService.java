package growdy.mumuri.login.service;

import growdy.mumuri.domain.Member;
import growdy.mumuri.login.CustomUserDetails;
import growdy.mumuri.login.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;

    @Override
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        // JWT에서 꺼낸 userId (String)을 Long으로 변환해서 DB 조회
        Member member = memberRepository.findById(Long.parseLong(userId))
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userId));

        // Member를 CustomUserDetails로 감싸서 반환
        return new CustomUserDetails(member);
    }
}
