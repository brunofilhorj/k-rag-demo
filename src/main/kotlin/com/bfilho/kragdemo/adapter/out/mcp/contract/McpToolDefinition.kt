package com.bfilho.kragdemo.adapter.out.mcp.contract

data class McpToolDefinition(
    val name: String,
    val description: String,
    val inputSchema: Map<String, Any>
)
