package com.omnixys.address.services;

import tools.jackson.databind.ObjectMapper;
import com.omnixys.address.models.dto.SignupAddressCacheDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AddressCacheService {

    private static final String SIGNUP_TOKEN_PREFIX = "verification:signup:address:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public SignupAddressCacheDTO getSignupAddressToken(String token) {
        String key = SIGNUP_TOKEN_PREFIX + token;
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) {
            log.warn("Signup address cache miss: token not found or expired");
            throw new IllegalArgumentException("Signup token expired or invalid");
        }
        log.debug("Signup address cache hit, parsing payload");
        try {
            return objectMapper.readValue(json, SignupAddressCacheDTO.class);
        } catch (Exception e) {
            log.error("Failed to parse signup address cache payload: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to parse token payload", e);
        }
    }

    public void deleteToken(String token) {
        log.debug("Deleting signup address cache token");
        redisTemplate.delete(SIGNUP_TOKEN_PREFIX + token);
    }
}
