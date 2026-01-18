package growdy.mumuri.login.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import growdy.mumuri.domain.ChatRoom;
import growdy.mumuri.domain.Couple;
import growdy.mumuri.domain.Member;
import growdy.mumuri.dto.LogoutRequest;
import growdy.mumuri.login.AuthGuard;
import growdy.mumuri.login.CustomUserDetails;
import growdy.mumuri.login.dto.AppleUserInfo;
import growdy.mumuri.login.dto.KakaoUserInfo;
import growdy.mumuri.login.dto.LoginTest;
import growdy.mumuri.login.jwt.JwtUtil;
import growdy.mumuri.login.service.MemberService;
import growdy.mumuri.repository.ChatRoomRepository;
import growdy.mumuri.repository.CoupleRepository;
import growdy.mumuri.service.AuthService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
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

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;

@RestController
@RequiredArgsConstructor
public class LoginController {

    private final MemberService memberService;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CoupleRepository coupleRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final AuthService authService;


    @Value("${kakao.redirect-uri}")
    private String redirectUri;

    @Value("${kakao.client-id}")
    private String clientId;

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


    @GetMapping("/api/auth/kakao/login")
    public String redirectToKakao() {
        String kakaoUrl = "https://kauth.kakao.com/oauth/authorize?response_type=code"
                + "&client_id=" + clientId
                + "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);
        return "redirect:" + kakaoUrl;
    }

    @GetMapping("/api/auth/apple/login")
    public ResponseEntity<Void> redirectToApple() {
        String appleUrl = "https://appleid.apple.com/auth/authorize?response_type=code"
                + "&client_id=" + appleClientId
                + "&redirect_uri=" + URLEncoder.encode(appleRedirectUri, StandardCharsets.UTF_8)
                + "&scope=name%20email"
                + "&response_mode=form_post";
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(appleUrl))
                .build();
    }
    @DeleteMapping("/api/auth/withdraw")
    public ResponseEntity<Void> withdraw(@AuthenticationPrincipal CustomUserDetails user) {
        memberService.withdraw(AuthGuard.requireUser(user).getId()); // 또는 WithdrawalService
        return ResponseEntity.noContent().build();
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
    public void kakaoCallback(
            @RequestParam String code,
            HttpServletResponse response
    ) throws IOException {
        // 1. 카카오에서 access token 가져오기
        String kakaoAccessToken = getAccessToken(code);

        // 2. 카카오 사용자 정보 조회
        String userInfoJson = getUserInfo(kakaoAccessToken);
        JsonNode userInfoNode = objectMapper.readTree(userInfoJson);
        KakaoUserInfo kakaoUser = KakaoUserInfo.from(userInfoNode);

        // 3. 우리 서비스에 Member 등록 or 기존 유저 조회
        var result = memberService.registerIfAbsent(kakaoUser);
        Member member = result.member();
        boolean isNew;
        isNew = member.getAnniversary() == null;

        // 4. 커플 / 채팅방 조회
        Couple couple = coupleRepository
                .findByMember1IdOrMember2Id(member.getId(), member.getId())
                .orElse(null);

        ChatRoom chatRoom = (couple != null)
                ? chatRoomRepository.findByCouple(couple).orElse(null)
                : null;

        Long roomId = (chatRoom != null) ? chatRoom.getId() : null;

        // 5. 우리 서비스 JWT(access + refresh) 발급 (실무용)
        //    user-agent나 ip 같은 건 여기서 넘기고 싶으면 HttpServletRequest도 파라미터로 받아서 넣어주면 됨
        var tokens = authService.issueTokens(member.getId(), null, null);
        String accessToken = tokens.accessToken();
        String refreshToken = tokens.refreshToken();

        String email = member.getEmail();       // 한글 가능
        String nickname = member.getNickname(); // 한글 가능

        // ⛔ 여기서 URLEncoder로 먼저 인코딩하면, 아래 build(true)가 또 인코딩해서 두 번 인코딩됨
        //    그래서 그냥 생 문자열을 넣고, UriComponentsBuilder에 맡기는 게 좋음.
        URI deeplink = UriComponentsBuilder
                .newInstance()
                .scheme("mumuri")
                .path("oauth/kakao")
                .queryParam("accessToken", accessToken)
                .queryParam("refreshToken", refreshToken)
                .queryParam("email", email)
                .queryParam("nickname", nickname)
                .queryParam("status", member.getStatus())
                .queryParam("roomId", roomId)
                .queryParam("isNew", isNew)
                .build(false)
                .encode(StandardCharsets.UTF_8)
                .toUri();
        response.sendRedirect(deeplink.toString());
    }

    @RequestMapping(value = "/api/auth/apple/callback", method = {RequestMethod.GET, RequestMethod.POST})
    public void appleCallback(
            @RequestParam(required = false) String code,
            @RequestBody(required = false) AppleCallbackRequest request,
            HttpServletResponse response
    ) throws IOException {
        String authorizationCode = (code != null && !code.isBlank())
                ? code
                : (request != null ? request.code() : null);

        if (authorizationCode == null || authorizationCode.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Apple 인증 code 파라미터가 필요합니다."
            );
        }

        String appleIdToken = getAppleIdToken(authorizationCode);
        JsonNode appleTokenPayload = decodeAppleIdToken(appleIdToken);
        AppleUserInfo appleUser = AppleUserInfo.from(appleTokenPayload);

        var result = memberService.registerIfAbsent(appleUser);
        Member member = result.member();
        boolean isNew = member.getAnniversary() == null;

        Couple couple = coupleRepository
                .findByMember1IdOrMember2Id(member.getId(), member.getId())
                .orElse(null);

        ChatRoom chatRoom = (couple != null)
                ? chatRoomRepository.findByCouple(couple).orElse(null)
                : null;

        Long roomId = (chatRoom != null) ? chatRoom.getId() : null;

        var tokens = authService.issueTokens(member.getId(), null, null);
        String accessToken = tokens.accessToken();
        String refreshToken = tokens.refreshToken();

        String email = member.getEmail();
        String nickname = member.getNickname();

        URI deeplink = UriComponentsBuilder
                .newInstance()
                .scheme("mumuri")
                .path("oauth/apple")
                .queryParam("accessToken", accessToken)
                .queryParam("refreshToken", refreshToken)
                .queryParam("email", email)
                .queryParam("nickname", nickname)
                .queryParam("status", member.getStatus())
                .queryParam("roomId", roomId)
                .queryParam("isNew", isNew)
                .build(false)
                .encode(StandardCharsets.UTF_8)
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

    private String getAppleIdToken(String code) throws JsonProcessingException {
        String tokenUrl = "https://appleid.apple.com/auth/token";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", appleClientId);
        params.add("client_secret", createAppleClientSecret());
        params.add("redirect_uri", appleRedirectUri);
        params.add("code", code);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        RestTemplate restTemplate = new RestTemplate();

        try {
            ResponseEntity<String> response =
                    restTemplate.exchange(tokenUrl, HttpMethod.POST, request, String.class);

            JsonNode json = objectMapper.readTree(response.getBody());
            JsonNode idTokenNode = json.get("id_token");

            if (idTokenNode == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Apple id_token이 응답에 없습니다."
                );
            }

            return idTokenNode.asText();

        } catch (HttpClientErrorException.TooManyRequests e) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Apple 요청이 너무 많습니다. 잠시 후 다시 시도해주세요."
            );

        } catch (HttpClientErrorException e) {
            String errorBody = e.getResponseBodyAsString();
            String message = "Apple 인증 실패: " + e.getStatusCode();
            if (errorBody != null && !errorBody.isBlank()) {
                message += " - " + errorBody;
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);

        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "서버 내부 오류 (Apple 토큰 요청 실패)"
            );
        }
    }

    private JsonNode decodeAppleIdToken(String idToken) throws JsonProcessingException {
        String[] parts = idToken.split("\\.");
        if (parts.length < 2) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Apple id_token 형식이 올바르지 않습니다."
            );
        }
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        return objectMapper.readTree(payload);
    }

    private String createAppleClientSecret() {
        Instant now = Instant.now();
        Date issuedAt = Date.from(now);
        Date expiresAt = Date.from(now.plusSeconds(300));

        return Jwts.builder()
                .setHeaderParam("kid", appleKeyId)
                .setIssuer(appleTeamId)
                .setAudience("https://appleid.apple.com")
                .setSubject(appleClientId)
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
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Apple private key 파싱 실패"
            );
        }
    }

    private record AppleCallbackRequest(String code) {
    }
}
