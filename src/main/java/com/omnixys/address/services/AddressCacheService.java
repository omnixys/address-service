package com.omnixys.address.services;

import tools.jackson.databind.ObjectMapper;
import com.omnixys.address.models.dto.SignupAddressCacheDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

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
            throw new IllegalArgumentException("Signup token expired or invalid");
        }
        try {
            return objectMapper.readValue(json, SignupAddressCacheDTO.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse token payload", e);
        }
    }

    public void deleteToken(String token) {
        redisTemplate.delete(SIGNUP_TOKEN_PREFIX + token);
    }
}
