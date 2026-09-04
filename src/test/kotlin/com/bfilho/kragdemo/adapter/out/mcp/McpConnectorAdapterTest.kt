package com.bfilho.kragdemo.adapter.out.mcp

import com.bfilho.kragdemo.adapter.out.mcp.config.McpConnectorProperties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestTemplate

class McpConnectorAdapterTest {

    @Test
    fun `should fetch external context from configured MCP tool`() {
        val properties = McpConnectorProperties().apply {
            enabled = true
            serverUrl = "http://localhost:8081"
            tools = listOf("jira.get_issue")
        }

        val restTemplate = RestTemplate()
        val mockServer = MockRestServiceServer.bindTo(restTemplate).build()
        mockServer.expect(requestTo("http://localhost:8081/mcp/tools/call"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess("""{"result":{"content":[{"type":"text","text":"Ticket ABC-123 is open and waiting for review."}]}}""", MediaType.APPLICATION_JSON))

        val adapter = McpConnectorAdapter(properties, restTemplate, com.bfilho.kragdemo.adapter.out.mcp.router.ToolIntentRouter())

        val facts = adapter.fetch("What is the status of ticket ABC-123?")

        assertEquals(1, facts.size)
        assertEquals("MCP", facts[0].source)
        assertEquals("jira.get_issue", facts[0].title)
        assertTrue(facts[0].content.contains("Ticket ABC-123 is open"))
        mockServer.verify()
    }

    @Test
    fun `should ignore JSON-RPC error payloads`() {
        val properties = McpConnectorProperties().apply {
            enabled = true
            serverUrl = "http://localhost:8081"
            tools = listOf("jira.get_issue")
        }

        val restTemplate = object : RestTemplate() {
            override fun <T : Any> postForEntity(
                url: String,
                request: Any?,
                responseType: Class<T>,
                vararg uriVariables: Any?
            ): ResponseEntity<T> {
                val payload = mapOf(
                    "jsonrpc" to "2.0",
                    "id" to 1,
                    "error" to mapOf(
                        "code" to -32601,
                        "message" to "Tool not found"
                    )
                )
                @Suppress("UNCHECKED_CAST")
                return ResponseEntity.ok(payload as T)
            }
        }

        val adapter = McpConnectorAdapter(properties, restTemplate, com.bfilho.kragdemo.adapter.out.mcp.router.ToolIntentRouter())
        val facts = adapter.fetch("O Bruno tem alguma issue?")

        assertTrue(facts.isEmpty())
    }

    @Test
    fun `should extract issue key from issueKey assignment syntax`() {
        val properties = McpConnectorProperties().apply {
            enabled = true
            serverUrl = "http://localhost:8081"
            tools = listOf("jira.get_issue")
        }

        val restTemplate = RestTemplate()
        val mockServer = MockRestServiceServer.bindTo(restTemplate).build()
        mockServer.expect(requestTo("http://localhost:8081/mcp/tools/call"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess("""{"result":{"content":[{"type":"text","text":"Issue abc-123: OPEN | owner=Bruno | status=In Review"}]}}""", MediaType.APPLICATION_JSON))

        val adapter = McpConnectorAdapter(properties, restTemplate, com.bfilho.kragdemo.adapter.out.mcp.router.ToolIntentRouter())
        val facts = adapter.fetch("O Bruno tem alguma issueKey=abc-123")

        assertEquals(1, facts.size)
        assertTrue(facts[0].content.contains("abc-123"))
        mockServer.verify()
    }

    @Test
    fun `should not call external tools when connector is disabled`() {
        val properties = McpConnectorProperties().apply {
            enabled = false
            serverUrl = "http://localhost:8081"
            tools = listOf("jira.get_issue")
        }

        val adapter = McpConnectorAdapter(properties, RestTemplate(), com.bfilho.kragdemo.adapter.out.mcp.router.ToolIntentRouter())

        val facts = adapter.fetch("What is the status of ticket ABC-123?")

        assertTrue(facts.isEmpty())
    }
}
