package org.remus.giteabot.ai.ollama;

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

class OllamaClientTest {

    private OllamaClient createClient() {
        RestClient restClient = mock(RestClient.class);
        return new OllamaClient(restClient, "llama3.2:1b", 1024, 10, 2, 6);
    }

    @Test
    void isPromptTooLongError_detectsTooLongError() {
        OllamaClient client = createClient();

        HttpClientErrorException ex = HttpClientErrorException.BadRequest.create(
                HttpStatusCode.valueOf(400),
                "Bad Request",
                HttpHeaders.EMPTY,
                "{\"error\":\"input is too long\"}".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8);

        assertTrue(client.isPromptTooLongError(ex));
    }

    @Test
    void isPromptTooLongError_detectsContextLengthError() {
        OllamaClient client = createClient();

        HttpClientErrorException ex = HttpClientErrorException.BadRequest.create(
                HttpStatusCode.valueOf(400),
                "Bad Request",
                HttpHeaders.EMPTY,
                "{\"error\":\"exceeds context length\"}".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8);

        assertTrue(client.isPromptTooLongError(ex));
    }

    @Test
    void isPromptTooLongError_ignoresUnrelatedErrors() {
        OllamaClient client = createClient();

        HttpClientErrorException ex = HttpClientErrorException.BadRequest.create(
                HttpStatusCode.valueOf(400),
                "Bad Request",
                HttpHeaders.EMPTY,
                "{\"error\":\"model not found\"}".getBytes(StandardCharsets.UTF_8),
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
        ArgumentCaptor<OllamaRequest> requestCaptor = ArgumentCaptor.forClass(OllamaRequest.class);
        when(restClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri("/api/chat")).thenReturn(bodySpec);
        when(bodySpec.body(requestCaptor.capture())).thenReturn(bodySpec);
        when(bodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(OllamaResponse.class)).thenReturn(response("ok"));
        OllamaClient client = new OllamaClient(restClient, "llama3.2:1b", 1024, 10, 2, 6);

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

    private OllamaResponse response(String text) {
        OllamaResponse response = new OllamaResponse();
        OllamaResponse.Message message = new OllamaResponse.Message();
        message.setRole("assistant");
        message.setContent(text);
        response.setMessage(message);
        return response;
    }
}
