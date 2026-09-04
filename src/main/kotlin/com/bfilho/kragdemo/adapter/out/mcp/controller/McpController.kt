package com.bfilho.kragdemo.adapter.out.mcp.controller

import com.bfilho.kragdemo.adapter.out.mcp.contract.McpToolCallRequest
import com.bfilho.kragdemo.adapter.out.mcp.McpToolRegistry
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/mcp")
class McpController(
    private val toolRegistry: McpToolRegistry
) {

    private val log = LoggerFactory.getLogger(McpController::class.java)

    @PostMapping(
        value = ["/tools/list"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun listTools(): ResponseEntity<Map<String, Any>> {
        val tools = toolRegistry.list().map { tool ->
            mapOf(
                "name" to tool.name,
                "description" to tool.description,
                "inputSchema" to tool.inputSchema
            )
        }

        return ResponseEntity.ok(
            mapOf(
                "jsonrpc" to "2.0",
                "id" to 1,
                "result" to mapOf("tools" to tools)
            )
        )
    }

    @PostMapping(
        value = ["/tools/call"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun callTool(@RequestBody request: McpToolCallRequest): ResponseEntity<Map<String, Any>> {
        val handler = toolRegistry.get(request.name)
        if (handler == null) {
            log.warn("MCP tool '{}' was not found.", request.name)
            return ResponseEntity.ok(
                mapOf(
                    "jsonrpc" to "2.0",
                    "id" to 1,
                    "error" to mapOf(
                        "code" to -32601,
                        "message" to "Tool not found: ${request.name}"
                    )
                )
            )
        }

        val output = try {
            handler.invoke(request.arguments)
        } catch (exception: Exception) {
            log.error("MCP tool '{}' failed to execute.", request.name, exception)
            return ResponseEntity.ok(
                mapOf(
                    "jsonrpc" to "2.0",
                    "id" to 1,
                    "error" to mapOf(
                        "code" to -32603,
                        "message" to "Tool execution failed: ${exception.message}"
                    )
                )
            )
        }

        return ResponseEntity.ok(
            mapOf(
                "jsonrpc" to "2.0",
                "id" to 1,
                "result" to mapOf(
                    "content" to listOf(
                        mapOf(
                            "type" to "text",
                            "text" to output
                        )
                    )
                )
            )
        )
    }
}
