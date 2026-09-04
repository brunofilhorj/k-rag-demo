package com.bfilho.kragdemo.adapter.out.mcp.tool

interface McpToolHandler {
    fun invoke(arguments: Map<String, Any>): String
}
