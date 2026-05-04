package org.remus.giteabot.ai.llamacpp;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.remus.giteabot.ai.McpConfigurationData;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LlamaCppClientTest {

    private LlamaCppClient createClient() {
        RestClient restClient = mock(RestClient.class);
        return new LlamaCppClient(restClient, "qwen2.5-coder-7b-instruct", 4096, 30000, 4, 15000);
    }

    @Test
    void isPromptTooLongError_detectsContextLengthError() {
        LlamaCppClient client = createClient();

        HttpClientErrorException ex = HttpClientErrorException.BadRequest.create(
                HttpStatusCode.valueOf(400),
                "Bad Request",
                HttpHeaders.EMPTY,
                "{\"error\":\"context length exceeded\"}".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8);

        assertTrue(client.isPromptTooLongError(ex));
    }

    @Test
    void isPromptTooLongError_detectsTooLongError() {
        LlamaCppClient client = createClient();

        HttpClientErrorException ex = HttpClientErrorException.BadRequest.create(
                HttpStatusCode.valueOf(400),
                "Bad Request",
                HttpHeaders.EMPTY,
                "{\"error\":\"input is too long\"}".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8);

        assertTrue(client.isPromptTooLongError(ex));
    }

    @Test
    void isPromptTooLongError_detectsMaximumContextError() {
        LlamaCppClient client = createClient();

        HttpClientErrorException ex = HttpClientErrorException.BadRequest.create(
                HttpStatusCode.valueOf(400),
                "Bad Request",
                HttpHeaders.EMPTY,
                "{\"error\":\"maximum context size reached\"}".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8);

        assertTrue(client.isPromptTooLongError(ex));
    }

    @Test
    void isPromptTooLongError_detectsTokenLimitError() {
        LlamaCppClient client = createClient();

        HttpClientErrorException ex = HttpClientErrorException.BadRequest.create(
                HttpStatusCode.valueOf(400),
                "Bad Request",
                HttpHeaders.EMPTY,
                "{\"error\":\"token limit exceeded\"}".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8);

        assertTrue(client.isPromptTooLongError(ex));
    }

    @Test
    void isPromptTooLongError_detectsExceedsError() {
        LlamaCppClient client = createClient();

        HttpClientErrorException ex = HttpClientErrorException.BadRequest.create(
                HttpStatusCode.valueOf(400),
                "Bad Request",
                HttpHeaders.EMPTY,
                "{\"error\":\"prompt exceeds model capacity\"}".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8);

        assertTrue(client.isPromptTooLongError(ex));
    }

    @Test
    void isPromptTooLongError_ignoresUnrelatedErrors() {
        LlamaCppClient client = createClient();

        HttpClientErrorException ex = HttpClientErrorException.BadRequest.create(
                HttpStatusCode.valueOf(400),
                "Bad Request",
                HttpHeaders.EMPTY,
                "{\"error\":\"model not found\"}".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8);

        assertFalse(client.isPromptTooLongError(ex));
    }

    @Test
    void isPromptTooLongError_handlesNullBody() {
        LlamaCppClient client = createClient();

        HttpClientErrorException ex = HttpClientErrorException.BadRequest.create(
                HttpStatusCode.valueOf(400),
                "Bad Request",
                HttpHeaders.EMPTY,
                null,
                StandardCharsets.UTF_8);

        assertFalse(client.isPromptTooLongError(ex));
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void chat_withMcpConfiguration_forwardsMcpServers() {
        RestClient restClient = mock(RestClient.class);
        RestClient.RequestBodyUriSpec uriSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
        ArgumentCaptor<LlamaCppRequest> requestCaptor = ArgumentCaptor.forClass(LlamaCppRequest.class);
        when(restClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri("/completion")).thenReturn(bodySpec);
        when(bodySpec.body(requestCaptor.capture())).thenReturn(bodySpec);
        when(bodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(LlamaCppResponse.class)).thenReturn(response("ok"));
        LlamaCppClient client = new LlamaCppClient(restClient, "qwen2.5-coder-7b-instruct",
                4096, 30000, 4, 15000);

        String result = client.chat(List.of(), "hello", "system", null, 100,
                new McpConfigurationData("GitHub MCP", """
                        {"name":"github","type":"url","url":"https://api.githubcopilot.com/mcp/"}
                        """));

        assertEquals("ok", result);
        assertNotNull(requestCaptor.getValue().getMcpServers());
        assertEquals(1, requestCaptor.getValue().getMcpServers().size());
        assertEquals("https://api.githubcopilot.com/mcp/",
                requestCaptor.getValue().getMcpServers().getFirst().get("url").asText());
    }

    private LlamaCppResponse response(String text) {
        LlamaCppResponse response = new LlamaCppResponse();
        response.setContent(text);
        return response;
    }
}
