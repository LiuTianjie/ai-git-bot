package org.remus.giteabot.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public final class McpConfigurationMapper {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private McpConfigurationMapper() {
    }

    public static List<JsonNode> toMcpServers(McpConfigurationData mcpConfiguration, String providerName) {
        if (mcpConfiguration == null || mcpConfiguration.json() == null || mcpConfiguration.json().isBlank()) {
            return null;
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(mcpConfiguration.json());
            List<JsonNode> servers = new ArrayList<>();
            if (root.isArray()) {
                root.forEach(servers::add);
            } else if (root.has("mcp_servers") && root.get("mcp_servers").isArray()) {
                root.get("mcp_servers").forEach(servers::add);
            } else if (root.has("mcpServers") && root.get("mcpServers").isArray()) {
                root.get("mcpServers").forEach(servers::add);
            } else {
                servers.add(root);
            }
            return servers;
        } catch (Exception e) {
            log.error("MCP configuration '{}' is not valid for {} requests; continuing without MCP: {}",
                    mcpConfiguration.name(), providerName, e.getMessage(), e);
            return null;
        }
    }
}
