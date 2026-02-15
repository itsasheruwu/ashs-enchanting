package com.example.ashsenchanting.update;

import com.example.ashsenchanting.AshsEnchanting;
import com.example.ashsenchanting.config.PluginSettings;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AutoUpdateService {
    private static final Pattern TAG_NAME_PATTERN = Pattern.compile("\"tag_name\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern PRERELEASE_PATTERN = Pattern.compile("\"prerelease\"\\s*:\\s*(true|false)");
    private static final Pattern ASSET_URL_PATTERN = Pattern.compile("\"browser_download_url\"\\s*:\\s*\"([^\"]+)\"");
    private static final String RELEASES_LATEST_API = "https://api.github.com/repos/%s/releases/latest";

    private final AshsEnchanting plugin;
    private final HttpClient httpClient;

    public AutoUpdateService(AshsEnchanting plugin) {
        this.plugin = plugin;
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public void checkAndDownloadUpdateAsync(PluginSettings settings) {
        if (!settings.autoUpdateEnabled()) {
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> checkAndDownloadUpdate(settings));
    }

    private void checkAndDownloadUpdate(PluginSettings settings) {
        String repo = settings.autoUpdateRepository().trim();
        if (repo.isBlank() || !repo.contains("/")) {
            plugin.getLogger().warning("Auto-update skipped: invalid autoUpdateRepository value: " + settings.autoUpdateRepository());
            return;
        }

        String currentVersion = normalizeVersion(plugin.getDescription().getVersion());
        ReleaseInfo latest = fetchLatestReleaseInfo(repo, settings.autoUpdateTimeoutSeconds());
        if (latest == null) {
            return;
        }

        if (latest.prerelease() && !settings.autoUpdateAllowPrerelease()) {
            plugin.logInfo("Auto-update skipped: latest release is prerelease (" + latest.tagName() + ").");
            return;
        }

        String latestVersion = normalizeVersion(latest.tagName());
        if (compareVersions(latestVersion, currentVersion) <= 0) {
            plugin.logInfo("Auto-update: already up to date (" + plugin.getDescription().getVersion() + ").");
            return;
        }

        String assetUrl = selectJarAssetUrl(latest.assetUrls());
        if (assetUrl == null) {
            plugin.getLogger().warning("Auto-update skipped: no .jar asset found for release " + latest.tagName());
            return;
        }

        try {
            Path target = resolveUpdateTargetPath();
            downloadAssetToTarget(assetUrl, target, settings.autoUpdateTimeoutSeconds());
            plugin.getLogger().info("Auto-update downloaded " + latest.tagName()
                    + " to " + target
                    + ". Restart the server to apply.");
        } catch (Exception exception) {
            plugin.getLogger().warning("Auto-update failed: " + exception.getMessage());
        }
    }

    private ReleaseInfo fetchLatestReleaseInfo(String repo, int timeoutSeconds) {
        String url = RELEASES_LATEST_API.formatted(repo);
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(Math.max(5, timeoutSeconds)))
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "AshsEnchanting-AutoUpdater")
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                plugin.getLogger().warning("Auto-update check failed (HTTP " + response.statusCode() + ") from GitHub.");
                return null;
            }

            String body = response.body();
            String tagName = extractFirst(TAG_NAME_PATTERN, body);
            if (tagName == null || tagName.isBlank()) {
                plugin.getLogger().warning("Auto-update check failed: release tag_name missing.");
                return null;
            }

            boolean prerelease = Boolean.parseBoolean(extractFirst(PRERELEASE_PATTERN, body));
            List<String> assetUrls = extractAll(ASSET_URL_PATTERN, body);
            return new ReleaseInfo(tagName, prerelease, assetUrls);
        } catch (Exception exception) {
            plugin.getLogger().warning("Auto-update check failed: " + exception.getMessage());
            return null;
        }
    }

    private Path resolveUpdateTargetPath() throws URISyntaxException {
        File updateFolder = plugin.getServer().getUpdateFolderFile();
        Path updateFolderPath = updateFolder.toPath();
        String currentJarName = resolveCurrentPluginJarName();

        try {
            Files.createDirectories(updateFolderPath);
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot create update folder: " + updateFolderPath, exception);
        }

        return updateFolderPath.resolve(currentJarName);
    }

    private String resolveCurrentPluginJarName() throws URISyntaxException {
        URI sourceUri = plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI();
        Path sourcePath = Path.of(sourceUri);
        String fileName = sourcePath.getFileName() == null ? null : sourcePath.getFileName().toString();
        if (fileName == null || fileName.isBlank() || !fileName.toLowerCase(Locale.ROOT).endsWith(".jar")) {
            return "ashs-enchanting.jar";
        }
        return fileName;
    }

    private void downloadAssetToTarget(String assetUrl, Path target, int timeoutSeconds) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(assetUrl))
                .timeout(Duration.ofSeconds(Math.max(5, timeoutSeconds)))
                .header("Accept", "application/octet-stream")
                .header("User-Agent", "AshsEnchanting-AutoUpdater")
                .GET()
                .build();

        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() >= 400) {
            throw new IllegalStateException("Download failed with HTTP " + response.statusCode());
        }

        Path temp = Files.createTempFile(target.getParent(), "ashs-enchanting-update-", ".jar");
        try (InputStream input = response.body()) {
            Files.copy(input, temp, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception exception) {
            Files.deleteIfExists(temp);
            throw exception;
        }

        Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    private String selectJarAssetUrl(List<String> assetUrls) {
        String preferred = null;
        for (String rawUrl : assetUrls) {
            String url = rawUrl.replace("\\/", "/");
            String lower = url.toLowerCase(Locale.ROOT);
            if (!lower.endsWith(".jar")) {
                continue;
            }
            if (lower.contains("ashs-enchanting")) {
                return url;
            }
            if (preferred == null) {
                preferred = url;
            }
        }
        return preferred;
    }

    private static String extractFirst(Pattern pattern, String input) {
        Matcher matcher = pattern.matcher(input);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private static List<String> extractAll(Pattern pattern, String input) {
        Matcher matcher = pattern.matcher(input);
        List<String> values = new ArrayList<>();
        while (matcher.find()) {
            values.add(matcher.group(1));
        }
        return values;
    }

    private static String normalizeVersion(String version) {
        if (version == null) {
            return "";
        }
        String normalized = version.trim();
        if (normalized.startsWith("v") || normalized.startsWith("V")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    private static int compareVersions(String left, String right) {
        String[] leftParts = left.split("\\.");
        String[] rightParts = right.split("\\.");
        int max = Math.max(leftParts.length, rightParts.length);

        for (int i = 0; i < max; i++) {
            int leftPart = i < leftParts.length ? leadingInt(leftParts[i]) : 0;
            int rightPart = i < rightParts.length ? leadingInt(rightParts[i]) : 0;
            if (leftPart != rightPart) {
                return Integer.compare(leftPart, rightPart);
            }
        }

        boolean leftPre = isPreRelease(left);
        boolean rightPre = isPreRelease(right);
        if (leftPre == rightPre) {
            return 0;
        }
        return leftPre ? -1 : 1;
    }

    private static int leadingInt(String token) {
        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);
            if (Character.isDigit(c)) {
                digits.append(c);
            } else {
                break;
            }
        }
        if (digits.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(digits.toString());
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static boolean isPreRelease(String version) {
        String lower = version.toLowerCase(Locale.ROOT);
        return lower.contains("snapshot")
                || lower.contains("alpha")
                || lower.contains("beta")
                || lower.contains("rc");
    }

    private record ReleaseInfo(String tagName, boolean prerelease, List<String> assetUrls) {
    }
}
