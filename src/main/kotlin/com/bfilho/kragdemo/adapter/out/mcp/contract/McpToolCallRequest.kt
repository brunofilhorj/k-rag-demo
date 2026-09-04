package com.bfilho.kragdemo.adapter.out.mcp.contract

data class McpToolCallRequest(
    val name: String,
    val arguments: Map<String, Any> = emptyMap()
)
