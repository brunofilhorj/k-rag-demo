package com.bfilho.kragdemo.adapter.out.mcp.tool

import org.springframework.stereotype.Component

@Component
class GithubSearchPrsTool : McpToolHandler {
    override fun invoke(arguments: Map<String, Any>): String {
        val query = arguments["query"] as? String ?: "project update"
        return "No open pull requests were found matching '$query'."
    }
}
