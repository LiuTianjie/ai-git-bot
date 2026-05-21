package org.remus.giteabot.ai.anthropic;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.util.Timeout;
import org.remus.giteabot.admin.AiIntegration;
import org.remus.giteabot.ai.AiClient;
import org.remus.giteabot.ai.AiProviderMetadata;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;

/**
 * Metadata and factory for Anthropic Claude AI integration.
 */
@Component
public class AnthropicProviderMetadata implements AiProviderMetadata {

    public static final String PROVIDER_TYPE = "anthropic";
    public static final String DEFAULT_API_URL = "https://api.anthropic.com";
    public static final String DEFAULT_API_VERSION = "2023-06-01";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration READ_TIMEOUT = Duration.ofMinutes(5);
    public static final List<String> SUGGESTED_MODELS = List.of(
            "claude-opus-4-7",
            "claude-sonnet-4-6",
            "claude-haiku-4-5-20251001"
    );

    @Override
    public String getProviderType() {
        return PROVIDER_TYPE;
    }

    @Override
    public String getDefaultApiUrl() {
        return DEFAULT_API_URL;
    }

    @Override
    public List<String> getSuggestedModels() {
        return SUGGESTED_MODELS;
    }

    @Override
    public boolean requiresApiKey() {
        return true;
    }

    @Override
    public RestClient buildRestClient(AiIntegration integration, String decryptedApiKey) {
        if (decryptedApiKey == null || decryptedApiKey.isBlank()) {
            throw new IllegalStateException("Anthropic integration requires an API key");
        }

        String apiVersion = integration.getApiVersion();
        if (apiVersion == null || apiVersion.isBlank()) {
            apiVersion = DEFAULT_API_VERSION;
        }

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.of(CONNECT_TIMEOUT))
                .setResponseTimeout(Timeout.of(READ_TIMEOUT))
                .build();
        CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build();
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
        requestFactory.setConnectionRequestTimeout(CONNECT_TIMEOUT);

        return RestClient.builder()
                .requestFactory(requestFactory)
                .baseUrl(integration.getApiUrl())
                .defaultHeader("x-api-key", decryptedApiKey)
                .defaultHeader("anthropic-version", apiVersion)
                .defaultHeader("anthropic-beta", "mcp-client-2025-11-20")
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Override
    public AiClient createClient(RestClient restClient, AiIntegration integration) {
        return new AnthropicAiClient(
                restClient,
                integration.getModel(),
                integration.getMaxTokens(),
                integration.getMaxDiffCharsPerChunk(),
                integration.getMaxDiffChunks(),
                integration.getRetryTruncatedChunkChars()
        );
    }
}
