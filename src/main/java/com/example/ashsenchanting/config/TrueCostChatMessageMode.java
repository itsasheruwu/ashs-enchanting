package com.example.ashsenchanting.config;

public enum TrueCostChatMessageMode {
    ALWAYS("always"),
    NEVER("never"),
    FALLBACK_ONLY("fallback-only");

    private final String configValue;

    TrueCostChatMessageMode(String configValue) {
        this.configValue = configValue;
    }

    public boolean shouldSend(boolean trueCostDisplayedInUi) {
        return switch (this) {
            case ALWAYS -> true;
            case NEVER -> false;
            case FALLBACK_ONLY -> !trueCostDisplayedInUi;
        };
    }

    public String configValue() {
        return configValue;
    }

    public static TrueCostChatMessageMode fromConfig(String rawValue) {
        if (rawValue == null) {
            return FALLBACK_ONLY;
        }

        String normalized = rawValue.trim().toLowerCase();
        for (TrueCostChatMessageMode mode : values()) {
            if (mode.configValue.equals(normalized)) {
                return mode;
            }
        }

        return FALLBACK_ONLY;
    }
}
