package com.bfilho.kragdemo.adapter.out.mcp.contract

data class McpToolResult(
    val toolName: String,
    val status: String,
    val summary: String,
    val details: Map<String, Any> = emptyMap()
) {
    fun toLlmContext(): String {
        val detailText = if (details.isEmpty()) {
            ""
        } else {
            "\nDetails: ${details.entries.joinToString(", ") { (key, value) -> "$key=$value" }}"
        }
        return "[Tool: $toolName]\nStatus: $status\nSummary: $summary$detailText"
    }
}
