package com.example.ashsenchanting.bedrock;

public final class BedrockDetectorFactory {
    private BedrockDetectorFactory() {
    }

    public static BedrockPlayerDetector create() {
        BedrockPlayerDetector floodgateDetector = FloodgateBedrockDetector.createIfAvailable();
        if (floodgateDetector != null) {
            return floodgateDetector;
        }

        BedrockPlayerDetector geyserDetector = GeyserBedrockDetector.createIfAvailable();
        if (geyserDetector != null) {
            return geyserDetector;
        }

        return new NoopBedrockDetector("none");
    }
}
