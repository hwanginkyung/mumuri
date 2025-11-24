package growdy.mumuri.login.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import growdy.mumuri.domain.Member;
import growdy.mumuri.login.dto.KakaoUserInfo;
import growdy.mumuri.login.dto.LoginTest;
import growdy.mumuri.login.jwt.JwtUtil;
import growdy.mumuri.login.service.MemberService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

@RestController
@RequiredArgsConstructor
public class LoginController {

    private final MemberService memberService;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper = new ObjectMapper();


    @Value("${kakao.redirect-uri}")
    private String redirectUri;

    @Value("${kakao.client-id}")
    private String clientId;


    @GetMapping("/api/auth/kakao/login")
    public String redirectToKakao() {
        String kakaoUrl = "https://kauth.kakao.com/oauth/authorize?response_type=code"
                + "&client_id=" + clientId
                + "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);
        return "redirect:" + kakaoUrl;
    }

    /*@GetMapping("/api/auth/kakao/callback")
    public ResponseEntity<LoginTest> kakaoCallback(@RequestParam String code) {
        System.out.println("check1");
        try {
            String accessToken = getAccessToken(code);
            String userInfoJson = getUserInfo(accessToken);
            JsonNode userInfoNode = objectMapper.readTree(userInfoJson);
            KakaoUserInfo kakaoUser = KakaoUserInfo.from(userInfoNode);

            // DB 등록 or 조회
            Member member = memberService.registerIfAbsent(kakaoUser);

            // JWT 발급
            String token = jwtUtil.createToken(member.getId());

            System.out.println(token);
            return ResponseEntity.ok(new LoginTest(token, member.getNickname()));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }*/
    @GetMapping("/api/auth/kakao/callback")
    public void kakaoCallback(@RequestParam String code, HttpServletResponse response) throws IOException {
        String accessToken = getAccessToken(code);
        String userInfoJson = getUserInfo(accessToken);
        JsonNode userInfoNode = objectMapper.readTree(userInfoJson);
        KakaoUserInfo kakaoUser = KakaoUserInfo.from(userInfoNode);

        // DB 등록 or 조회
        Member member = memberService.registerIfAbsent(kakaoUser);

        // JWT 발급
        String token = jwtUtil.createToken(member.getId());

        String email = member.getEmail();       // 한글 가능
        String nickname = member.getNickname(); // 한글 가능

        String encodedEmail = URLEncoder.encode(email, StandardCharsets.UTF_8);
        String encodedNickname = URLEncoder.encode(nickname, StandardCharsets.UTF_8);
        System.out.println(token);
        URI deeplink = UriComponentsBuilder
                .newInstance()
                .scheme("mumuri")
                .path("oauth/kakao")   // path는 맨 앞 / 없이
                .queryParam("token", token)
                .queryParam("email", encodedEmail)
                .queryParam("nickname", encodedNickname)
                .queryParam("status", member.getStatus())
                .build(true)           // 쿼리 파라미터까지 인코딩 보장
                .toUri();
        response.sendRedirect(deeplink.toString());
    }


    private String getAccessToken(String code) throws JsonProcessingException {

        String tokenUrl = "https://kauth.kakao.com/oauth/token";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        params.add("redirect_uri", redirectUri);
        params.add("code", code);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        RestTemplate restTemplate = new RestTemplate();

        try {
            ResponseEntity<String> response =
                    restTemplate.exchange(tokenUrl, HttpMethod.POST, request, String.class);

            System.out.println("카카오 토큰 응답: " + response.getBody());

            JsonNode json = objectMapper.readTree(response.getBody());
            JsonNode accessTokenNode = json.get("access_token");

            if (accessTokenNode == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "카카오 access_token이 응답에 없습니다."
                );
            }

            return accessTokenNode.asText();

        } catch (HttpClientErrorException.TooManyRequests e) {
            // 🔥 카카오 rate limit 초과 (429)
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "카카오 요청이 너무 많습니다. 잠시 후 다시 시도해주세요."
            );

        } catch (HttpClientErrorException e) {
            // 🔥 카카오가 400/401/403 등 다른 에러를 준 경우
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "카카오 인증 실패: " + e.getStatusCode()
            );

        } catch (Exception e) {
            // 🔥 예상 못한 에러는 500으로 감싸기
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "서버 내부 오류 (카카오 토큰 요청 실패)"
            );
        }
    }


    public String getUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.exchange(
                "https://kapi.kakao.com/v2/user/me",
                HttpMethod.GET,
                entity,
                String.class
        );
        return response.getBody(); // 사용자 정보 JSON
    }
}