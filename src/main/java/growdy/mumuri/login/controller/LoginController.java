package growdy.mumuri.login.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import growdy.mumuri.domain.ChatRoom;
import growdy.mumuri.domain.Couple;
import growdy.mumuri.domain.Member;
import growdy.mumuri.login.AuthGuard;
import growdy.mumuri.login.CustomUserDetails;
import growdy.mumuri.login.dto.AppleUserInfo;
import growdy.mumuri.login.dto.KakaoUserInfo;
import growdy.mumuri.login.jwt.JwtUtil;
import growdy.mumuri.login.service.MemberService;
import growdy.mumuri.repository.ChatRoomRepository;
import growdy.mumuri.repository.CoupleRepository;
import growdy.mumuri.service.AuthService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.*;

@RestController
@RequiredArgsConstructor
public class LoginController {

    private static final Logger log = LoggerFactory.getLogger(LoginController.class);
    private static final String KAKAO_REVIEW_BYPASS_EMAIL = "cjy031212@gmail.com";

    private final MemberService memberService;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CoupleRepository coupleRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final AuthService authService;

    @Value("${kakao.redirect-uri}")
    private String kakaoRedirectUri;

    @Value("${kakao.client-id}")
    private String kakaoClientId;

    @Value("${apple.client-id}")
    private String appleClientId;

    @Value("${apple.redirect-uri}")
    private String appleRedirectUri;

    @Value("${apple.team-id}")
    private String appleTeamId;

    @Value("${apple.key-id}")
    private String appleKeyId;

    @Value("${apple.private-key}")
    private String applePrivateKey;

    // =========================
    // DTO (응답)
    // =========================
    public record OAuthResult(
            String accessToken,
            String refreshToken,
            String email,
            String nickname,
            String status,
            Long roomId,
            boolean isNew
    ) {}

    // =========================
    // (선택) 서버가 애플/카카오 로그인 URL로 302 보내기
    // RestController에서는 "redirect:" 문자열이 redirect가 아니라 BODY로 나가니까 ResponseEntity로 처리
    // =========================
    @GetMapping("/api/auth/kakao/login")
    public ResponseEntity<Void> redirectToKakao() {
        String kakaoUrl = "https://kauth.kakao.com/oauth/authorize?response_type=code"
                + "&client_id=" + kakaoClientId
                + "&redirect_uri=" + URLEncoder.encode(kakaoRedirectUri, StandardCharsets.UTF_8);

        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(kakaoUrl)).build();
    }

    @GetMapping("/api/auth/apple/login")
    public ResponseEntity<Void> redirectToApple() {
        // name/email scope 쓸 때는 response_mode=form_post 필수
        String appleUrl = "https://appleid.apple.com/auth/authorize?response_type=code"
                + "&client_id=" + appleClientId
                + "&redirect_uri=" + URLEncoder.encode(appleRedirectUri, StandardCharsets.UTF_8)
                + "&scope=name%20email"
                + "&response_mode=form_post";

        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(appleUrl)).build();
    }

    @DeleteMapping("/api/auth/withdraw")
    public ResponseEntity<Void> withdraw(@AuthenticationPrincipal CustomUserDetails user) {
        memberService.withdraw(AuthGuard.requireUser(user).getId());
        return ResponseEntity.noContent().build();
    }

    // =========================
    // Kakao callback
    // - 브라우저/웹뷰: 302 mumuri://...
    // - 앱 axios: 200 JSON
    // =========================
    @GetMapping("/api/auth/kakao/callback")
    public ResponseEntity<?> kakaoCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String reviewEmail,
            HttpServletRequest request
    ) throws JsonProcessingException {

        Member member;

        if (KAKAO_REVIEW_BYPASS_EMAIL.equalsIgnoreCase(Optional.ofNullable(reviewEmail).orElse(""))) {
            member = memberService.findByEmail(KAKAO_REVIEW_BYPASS_EMAIL)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "리뷰용 테스트 계정을 찾을 수 없습니다."));
            log.warn("Kakao auth-code verification skipped for review account: {}", KAKAO_REVIEW_BYPASS_EMAIL);

        } else {
            if (code == null || code.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Kakao authorization code가 없습니다.");
            }

            String kakaoAccessToken = getKakaoAccessToken(code);

            String userInfoJson = getKakaoUserInfo(kakaoAccessToken);
            JsonNode userInfoNode = objectMapper.readTree(userInfoJson);
            KakaoUserInfo kakaoUser = KakaoUserInfo.from(userInfoNode);

            var result = memberService.registerIfAbsent(kakaoUser);
            member = result.member();
        }

        boolean isNew = member.getAnniversary() == null;

        Long roomId = findRoomId(member.getId());

        var tokens = authService.issueTokens(member.getId(), null, null);

        OAuthResult payload = new OAuthResult(
                tokens.accessToken(),
                tokens.refreshToken(),
                member.getEmail(),
                member.getNickname(),
                String.valueOf(member.getStatus()),
                roomId,
                isNew
        );

        if (isAppAxiosCall(request)) {
            // ✅ 앱(axios) 호출이면 JSON으로 준다 (network error 방지)
            return ResponseEntity.ok(payload);
        }

        // ✅ 브라우저/웹뷰 플로우면 딥링크로 302
        URI deeplink = buildDeeplink("kakao", payload);
        return ResponseEntity.status(HttpStatus.FOUND).location(deeplink).build();
    }

    // =========================
    // Apple callback (GET/POST 둘 다)
    // - 브라우저/웹뷰: 302 mumuri://...
    // - 앱 axios: 200 JSON
    // =========================
    @RequestMapping(value = "/api/auth/apple/callback", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<?> appleCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String error,
            @RequestParam(name = "error_description", required = false) String errorDescription,
            HttpServletRequest request
    ) throws JsonProcessingException {

        log.debug("Apple callback: method={} hasCode={} error={}",
                request.getMethod(), code != null && !code.isBlank(), error);

        if (error != null && !error.isBlank()) {
            String msg = "Apple 인증 실패: " + error + (errorDescription != null ? " - " + errorDescription : "");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
        }
        if (code == null || code.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Apple authorization code가 없습니다.");
        }

        String appleIdToken = getAppleIdToken(code);           // ✅ 여기서 Apple /auth/token 교환
        JsonNode appleTokenPayload = decodeAppleIdToken(appleIdToken);
        AppleUserInfo appleUser = AppleUserInfo.from(appleTokenPayload);

        var result = memberService.registerIfAbsent(appleUser);
        Member member = result.member();
        boolean isNew = member.getAnniversary() == null;

        Long roomId = findRoomId(member.getId());

        var tokens = authService.issueTokens(member.getId(), null, null);

        OAuthResult payload = new OAuthResult(
                tokens.accessToken(),
                tokens.refreshToken(),
                member.getEmail(),
                member.getNickname(),
                String.valueOf(member.getStatus()),
                roomId,
                isNew
        );

        if (isAppAxiosCall(request)) {
            // ✅ 앱(axios) 호출이면 JSON
            return ResponseEntity.ok(payload);
        }

        // ✅ 브라우저/웹뷰 플로우면 딥링크로 302
        URI deeplink = buildDeeplink("apple", payload);
        return ResponseEntity.status(HttpStatus.FOUND).location(deeplink).build();
    }

    // =========================
    // Helpers
    // =========================
    private Long findRoomId(Long memberId) {
        Couple couple = coupleRepository
                .findByMember1IdOrMember2Id(memberId, memberId)
                .orElse(null);

        if (couple == null) return null;

        ChatRoom chatRoom = chatRoomRepository.findByCouple(couple).orElse(null);
        return chatRoom != null ? chatRoom.getId() : null;
    }

    private URI buildDeeplink(String provider, OAuthResult payload) {
        return UriComponentsBuilder
                .newInstance()
                .scheme("mumuri")
                .path("oauth/" + provider)
                .queryParam("accessToken", payload.accessToken())
                .queryParam("refreshToken", payload.refreshToken())
                .queryParam("email", payload.email())
                .queryParam("nickname", payload.nickname())
                .queryParam("status", payload.status())
                .queryParam("roomId", payload.roomId())
                .queryParam("isNew", payload.isNew())
                .build(false)
                .encode(StandardCharsets.UTF_8)
                .toUri();
    }

    /**
     * ✅ axios/앱 호출인지 판단
     * - axios 기본 accept: application/json, text/plain, *\/*
     * - 브라우저는 보통 text/html 포함
     */
    private boolean isAppAxiosCall(HttpServletRequest request) {
        String ua = Optional.ofNullable(request.getHeader("User-Agent")).orElse("");
        String accept = Optional.ofNullable(request.getHeader("Accept")).orElse("");
        String ct = Optional.ofNullable(request.getContentType()).orElse("");

        boolean looksLikeBrowser = ua.contains("Mozilla") && accept.contains("text/html");
        boolean looksLikeAxios = accept.contains("application/json")
                || ua.toLowerCase().contains("okhttp")
                || ua.toLowerCase().contains("cfnetwork")
                || ua.toLowerCase().contains("axios")
                || ua.toLowerCase().contains("reactnative")
                || ua.toLowerCase().contains("dart");

        // 애플 form_post는 브라우저에서 POST + x-www-form-urlencoded로 들어오는 경우가 많음
        if (looksLikeBrowser && ct.contains("application/x-www-form-urlencoded")) return false;

        return looksLikeAxios || !looksLikeBrowser;
    }

    // =========================
    // Kakao token exchange
    // =========================
    private String getKakaoAccessToken(String code) throws JsonProcessingException {
        String tokenUrl = "https://kauth.kakao.com/oauth/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", kakaoClientId);
        params.add("redirect_uri", kakaoRedirectUri);
        params.add("code", code);

        HttpEntity<MultiValueMap<String, String>> req = new HttpEntity<>(params, headers);
        RestTemplate restTemplate = new RestTemplate();

        try {
            ResponseEntity<String> res = restTemplate.exchange(tokenUrl, HttpMethod.POST, req, String.class);
            JsonNode json = objectMapper.readTree(res.getBody());
            JsonNode accessTokenNode = json.get("access_token");

            if (accessTokenNode == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "카카오 access_token이 응답에 없습니다.");
            }
            return accessTokenNode.asText();

        } catch (HttpClientErrorException.TooManyRequests e) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "카카오 요청이 너무 많습니다. 잠시 후 다시 시도해주세요.");
        } catch (HttpClientErrorException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "카카오 인증 실패: " + e.getStatusCode());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류 (카카오 토큰 요청 실패)");
        }
    }

    private String getKakaoUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.exchange(
                "https://kapi.kakao.com/v2/user/me",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );
        return response.getBody();
    }

    // =========================
    // Apple token exchange
    // =========================
    private String getAppleIdToken(String code) throws JsonProcessingException {
        String tokenUrl = "https://appleid.apple.com/auth/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", appleClientId);
        params.add("client_secret", createAppleClientSecret());
        // ✅ web 플로우면 redirect_uri 필수. (네가 설정해둔 값과 "정확히" 같아야 함)
        if (appleRedirectUri != null && !appleRedirectUri.isBlank()) {
            params.add("redirect_uri", appleRedirectUri);
        }
        params.add("code", code);

        HttpEntity<MultiValueMap<String, String>> req = new HttpEntity<>(params, headers);
        RestTemplate restTemplate = new RestTemplate();

        try {
            ResponseEntity<String> res = restTemplate.exchange(tokenUrl, HttpMethod.POST, req, String.class);

            JsonNode json = objectMapper.readTree(res.getBody());
            JsonNode idTokenNode = json.get("id_token");

            if (idTokenNode == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Apple id_token이 응답에 없습니다.");
            }
            return idTokenNode.asText();

        } catch (HttpClientErrorException e) {
            String body = e.getResponseBodyAsString();
            log.warn("Apple token request failed: status={} body={}", e.getStatusCode(), body);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Apple 인증 실패: " + e.getStatusCode() + (body != null && !body.isBlank() ? " - " + body : ""));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류 (Apple 토큰 요청 실패)");
        }
    }

    private JsonNode decodeAppleIdToken(String idToken) throws JsonProcessingException {
        String[] parts = idToken.split("\\.");
        if (parts.length < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Apple id_token 형식이 올바르지 않습니다.");
        }
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        return objectMapper.readTree(payload);
    }

    private String createAppleClientSecret() {
        Instant now = Instant.now();
        Date issuedAt = Date.from(now);
        Date expiresAt = Date.from(now.plusSeconds(300)); // 5분

        return Jwts.builder()
                .setHeaderParam("kid", appleKeyId)
                .setIssuer(appleTeamId)
                .setAudience("https://appleid.apple.com")
                .setSubject(appleClientId) // ✅ 여기 client_id와 반드시 일치해야 함
                .setIssuedAt(issuedAt)
                .setExpiration(expiresAt)
                .signWith(getApplePrivateKey(), SignatureAlgorithm.ES256)
                .compact();
    }

    private ECPrivateKey getApplePrivateKey() {
        try {
            String normalizedKey = applePrivateKey
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");

            byte[] decoded = Base64.getDecoder().decode(normalizedKey);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);

            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            return (ECPrivateKey) keyFactory.generatePrivate(keySpec);

        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Apple private key 파싱 실패");
        }
    }
}
