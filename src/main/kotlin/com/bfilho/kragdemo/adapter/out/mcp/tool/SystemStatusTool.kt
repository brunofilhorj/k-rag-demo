package com.bfilho.kragdemo.adapter.out.mcp.tool

import org.springframework.stereotype.Component

@Component
class SystemStatusTool : McpToolHandler {
    override fun invoke(arguments: Map<String, Any>): String {
        return "Service status: API online, knowledge base available, MCP connector enabled."
    }
}
