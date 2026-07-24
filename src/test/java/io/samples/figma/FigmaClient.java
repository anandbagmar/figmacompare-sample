package io.samples.figma;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class FigmaClient {

    private static final String FIGMA_API_BASE = "https://api.figma.com/v1";
    private static final Gson GSON = new Gson();
    // Figma renders the export server-side on first request, which can take well over
    // OkHttp's 10s default read timeout for large/complex frames.
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(30))
            .readTimeout(Duration.ofSeconds(60))
            .writeTimeout(Duration.ofSeconds(30))
            .build();
    private final String figmaToken;

    public FigmaClient(String figmaToken) {
        if (null == figmaToken || figmaToken.isBlank()) {
            throw new IllegalArgumentException("Figma token must not be null/blank");
        }
        this.figmaToken = figmaToken;
    }

    public File getCachedImage(String figmaUrl, String format, String scale, String cacheDir, boolean forceRefresh) {
        FigmaUrlInfo urlInfo = FigmaUrlParser.parse(figmaUrl);
        String sanitizedNodeId = urlInfo.getNodeId().replace(":", "-");
        File cacheFile = new File(cacheDir,
                urlInfo.getFileKey() + "_" + sanitizedNodeId + "_" + scale + "x." + format);

        if (cacheFile.exists() && !forceRefresh) {
            System.out.println("Using cached Figma image: " + cacheFile.getAbsolutePath());
            return cacheFile;
        }

        String imageUrl = fetchImageUrl(urlInfo.getFileKey(), urlInfo.getNodeId(), format, scale);
        downloadTo(imageUrl, cacheFile);
        return cacheFile;
    }

    public String fetchNodeName(String figmaUrl) {
        FigmaUrlInfo urlInfo = FigmaUrlParser.parse(figmaUrl);
        String url = FIGMA_API_BASE + "/files/" + urlInfo.getFileKey() + "/nodes?ids=" + urlInfo.getNodeId();
        JsonObject response = get(url);
        JsonObject nodes = response.getAsJsonObject("nodes");
        JsonObject node = nodes.getAsJsonObject(urlInfo.getNodeId());
        if (null == node) {
            throw new RuntimeException("Figma node " + urlInfo.getNodeId() + " not found in file "
                    + urlInfo.getFileKey());
        }
        return node.getAsJsonObject("document").get("name").getAsString();
    }

    private String fetchImageUrl(String fileKey, String nodeId, String format, String scale) {
        String url = FIGMA_API_BASE + "/images/" + fileKey + "?ids=" + nodeId + "&format=" + format + "&scale="
                + scale;
        JsonObject response = get(url);
        if (response.has("err") && !response.get("err").isJsonNull()) {
            throw new RuntimeException("Figma image API error for node " + nodeId + ": " + response.get("err"));
        }
        JsonObject images = response.getAsJsonObject("images");
        for (Map.Entry<String, com.google.gson.JsonElement> entry : images.entrySet()) {
            if (entry.getKey().equals(nodeId) && !entry.getValue().isJsonNull()) {
                return entry.getValue().getAsString();
            }
        }
        throw new RuntimeException("Figma did not return a renderable image URL for node " + nodeId);
    }

    private JsonObject get(String url) {
        Request request = new Request.Builder()
                .url(url)
                .header("X-Figma-Token", figmaToken)
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new RuntimeException("Figma API call failed [" + response.code() + "]: " + url);
            }
            return GSON.fromJson(response.body().string(), JsonObject.class);
        } catch (IOException ex) {
            throw new RuntimeException("Figma API call failed: " + url, ex);
        }
    }

    private void downloadTo(String imageUrl, File destination) {
        Request request = new Request.Builder().url(imageUrl).build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new RuntimeException("Failed to download Figma image [" + response.code() + "]: " + imageUrl);
            }
            Files.createDirectories(destination.getParentFile().toPath());
            try (FileOutputStream out = new FileOutputStream(destination)) {
                out.write(response.body().bytes());
            }
            System.out.println("Downloaded Figma image to cache: " + destination.getAbsolutePath());
        } catch (IOException ex) {
            throw new RuntimeException("Failed to download Figma image: " + imageUrl, ex);
        }
    }
}
