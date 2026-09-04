package com.bfilho.kragdemo.adapter.out.mcp

import com.bfilho.kragdemo.adapter.out.mcp.config.McpConnectorProperties
import com.bfilho.kragdemo.adapter.out.mcp.contract.McpToolDecision
import com.bfilho.kragdemo.adapter.out.mcp.contract.McpToolResult
import com.bfilho.kragdemo.adapter.out.mcp.router.ToolIntentRouter
import com.bfilho.kragdemo.domain.model.ExternalContextFact
import com.bfilho.kragdemo.domain.port.out.ExternalContextPort
import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate

@Component
class McpConnectorAdapter(
    private val properties: McpConnectorProperties,
    private val restTemplate: RestTemplate,
    private val toolRouter: ToolIntentRouter
) : ExternalContextPort {

    private val log = LoggerFactory.getLogger(McpConnectorAdapter::class.java)

    override fun fetch(question: String): List<ExternalContextFact> {
        if (!properties.enabled) {
            return emptyList()
        }

        val baseUrl = properties.serverUrl.trimEnd('/')
        if (baseUrl.isBlank()) {
            log.warn("MCP connector is enabled but no server URL has been configured.")
            return emptyList()
        }

        val decisions = toolRouter.route(question)
            .filter { it.toolName in properties.tools }

        return decisions.mapNotNull { decision ->
            try {
                invokeTool(baseUrl, decision)
            } catch (exception: RestClientException) {
                log.warn("MCP tool '{}' could not be executed against '{}'. Reason: {}", decision.toolName, baseUrl, exception.message)
                null
            }
        }
            .filter { it.content.isNotBlank() }
    }

    private fun invokeTool(baseUrl: String, decision: McpToolDecision): ExternalContextFact? {
        val requestBody = mapOf(
            "name" to decision.toolName,
            "arguments" to decision.arguments
        )
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
        }

        val attemptUrls = listOf(
            "$baseUrl/mcp/tools/call",
            "$baseUrl/tools/call",
            "$baseUrl/mcp/tools/${decision.toolName}/invoke",
            "$baseUrl/tools/${decision.toolName}/execute"
        )

        for (candidateUrl in attemptUrls) {
            try {
                val response = restTemplate.postForEntity(
                    candidateUrl,
                    HttpEntity(requestBody, headers),
                    Map::class.java
                )

                if (response.statusCode.is2xxSuccessful) {
                    val result = normalizeToolResult(decision.toolName, response)
                    if (result != null) {
                        return ExternalContextFact(
                            source = "MCP",
                            title = decision.toolName,
                            content = result.toLlmContext()
                        )
                    }
                }
            } catch (exception: RestClientException) {
                log.debug("MCP attempt failed for URL {}: {}", candidateUrl, exception.message)
            }
        }

        return null
    }

    private fun normalizeToolResult(toolName: String, response: ResponseEntity<Map<*, *>>): McpToolResult? {
        val payload = response.body ?: return null
        val error = payload["error"]
        if (error != null) {
            return null
        }

        val extracted = findTextValue(payload["result"] ?: payload)
        if (extracted == null) {
            return null
        }

        return McpToolResult(
            toolName = toolName,
            status = "success",
            summary = extracted,
            details = mapOf("source" to "MCP")
        )
    }

    private fun findTextValue(value: Any?): String? {
        if (value == null) {
            return null
        }

        return when (value) {
            is Map<*, *> -> {
                val directKeys = listOf("result", "output", "answer", "content", "text", "data")
                for (key in directKeys) {
                    val candidate = value[key]
                    val extracted = findTextValue(candidate)
                    if (extracted != null) {
                        return extracted
                    }
                }

                for (entry in value.entries) {
                    if (entry.key in setOf("jsonrpc", "id", "error")) {
                        continue
                    }
                    val extracted = findTextValue(entry.value)
                    if (extracted != null) {
                        return extracted
                    }
                }
                null
            }
            is Collection<*> -> {
                value.firstNotNullOfOrNull { findTextValue(it) }
            }
            is String -> value.takeIf { it.isNotBlank() }
            else -> value.toString().takeIf { it.isNotBlank() }
        }
    }
}
