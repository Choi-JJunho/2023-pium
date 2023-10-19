package com.official.pium.controller;

import com.official.pium.domain.Auth;
import com.official.pium.domain.Member;
import com.official.pium.exception.AuthenticationException;
import com.official.pium.repository.MemberRepository;
import com.official.pium.service.SessionGroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@RequiredArgsConstructor
public class MemberArgumentResolver implements HandlerMethodArgumentResolver {

    private static final int VALUE_INDEX = 1;
    private static final String SESSION_DELIMITER = "=";
    private static final String SESSION_KEY = "KAKAO_ID";
    private static final String COOKIE_HEADER = "cookie";
    private static final int MIN_SESSION_SIZE = 2;

    private final MemberRepository memberRepository;
    private final SessionGroupService sessionGroupService;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(Auth.class) && parameter.getParameterType().equals(Member.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory)
            throws AuthenticationException {
        String sessionValue = getSessionId(webRequest);
        try {
            Long kakaoId = Long.valueOf(sessionValue);
            return memberRepository.findByKakaoId(kakaoId)
                    .orElseThrow(() -> new AuthenticationException("회원을 찾을 수 없습니다."));
        } catch (NumberFormatException e) {
            throw new AuthenticationException("잘못된 세션 정보로 인해 사용자 인증에 실패하였습니다.");
        }
    }

    private String getSessionId(NativeWebRequest webRequest) {
        String sessionCookie = webRequest.getHeader(COOKIE_HEADER);
        String sessionId = parseSessionCookie(sessionCookie);
        return sessionGroupService.findOrExtendsBySessionIdAndKey(sessionId, SESSION_KEY);
    }

    private String parseSessionCookie(String sessionCookie) {
        if (sessionCookie == null || sessionCookie.isBlank()) {
            return null;
        }
        String[] keyAndValue = sessionCookie.split(SESSION_DELIMITER);
        if (keyAndValue.length < MIN_SESSION_SIZE) {
            return null;
        }
        return keyAndValue[VALUE_INDEX];
    }
}
