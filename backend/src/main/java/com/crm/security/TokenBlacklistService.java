package com.crm.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.crm.entity.TokenBlacklist;
import com.crm.mapper.TokenBlacklistMapper;
import com.crm.util.TokenUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final TokenBlacklistMapper tokenBlacklistMapper;

    /**
     * 登出时拉黑 Token，到期时间与 JWT 本身一致
     */
    public void revoke(String token, java.util.Date expireAt) {
        if (token == null || token.isBlank() || expireAt == null) {
            return;
        }
        String hash = TokenUtil.sha256(token);
        Long exists = tokenBlacklistMapper.selectCount(
                new LambdaQueryWrapper<TokenBlacklist>().eq(TokenBlacklist::getTokenHash, hash));
        if (exists != null && exists > 0) {
            return;
        }
        TokenBlacklist item = new TokenBlacklist();
        item.setTokenHash(hash);
        item.setExpireTime(expireAt.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
        tokenBlacklistMapper.insert(item);
    }

    public boolean isRevoked(String token) {
        if (token == null) {
            return false;
        }
        Long count = tokenBlacklistMapper.selectCount(
                new LambdaQueryWrapper<TokenBlacklist>()
                        .eq(TokenBlacklist::getTokenHash, TokenUtil.sha256(token)));
        return count != null && count > 0;
    }

    /**
     * 每小时清理已过期记录，避免表无限增长
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void cleanExpired() {
        int deleted = tokenBlacklistMapper.delete(
                new LambdaQueryWrapper<TokenBlacklist>().lt(TokenBlacklist::getExpireTime, LocalDateTime.now()));
        if (deleted > 0) {
            log.info("清理过期 Token 黑名单 {} 条", deleted);
        }
    }
}
