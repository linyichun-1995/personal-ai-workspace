package com.example.workspace.common.util;

import com.example.workspace.common.exception.BusinessException;

public final class AvatarUrls {

    private AvatarUrls() {
    }

    public static String normalize(String avatarUrl) {
        if (avatarUrl == null || avatarUrl.isBlank()) {
            return null;
        }
        String trimmed = avatarUrl.trim();
        if (!(trimmed.startsWith("https://") || trimmed.startsWith("http://"))) {
            throw new BusinessException("头像地址必须是 http 或 https URL");
        }
        if (trimmed.length() > 1000) {
            throw new BusinessException("头像地址过长");
        }
        return trimmed;
    }
}
