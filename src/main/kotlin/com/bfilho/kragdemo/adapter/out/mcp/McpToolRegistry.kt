package com.bfilho.kragdemo.adapter.out.mcp

import com.bfilho.kragdemo.adapter.out.mcp.contract.McpToolDefinition
import com.bfilho.kragdemo.adapter.out.mcp.tool.McpToolHandler
import org.springframework.stereotype.Component

@Component
class McpToolRegistry {
    private val tools = linkedMapOf<String, McpToolHandler>()

    fun register(name: String, handler: McpToolHandler) {
        tools[name] = handler
    }

    fun list(): List<McpToolDefinition> {
        return tools.map { (name, _) ->
            when (name) {
                "jira.get_issue" -> McpToolDefinition(
                    name = name,
                    description = "Returns the details for a specific Jira issue using a key like ABC-123.",
                    inputSchema = mapOf(
                        "type" to "object",
                        "properties" to mapOf(
                            "issueKey" to mapOf(
                                "type" to "string",
                                "description" to "The Jira key to look up."
                            )
                        ),
                        "required" to listOf("issueKey")
                    )
                )
                "jira.list_issues" -> McpToolDefinition(
                    name = name,
                    description = "Lists Jira issues filtered by assignee and status.",
                    inputSchema = mapOf(
                        "type" to "object",
                        "properties" to mapOf(
                            "assignee" to mapOf(
                                "type" to "string",
                                "description" to "Optional assignee name."
                            ),
                            "status" to mapOf(
                                "type" to "string",
                                "description" to "Optional status filter, such as OPEN, CLOSED, or IN_REVIEW."
                            )
                        ),
                        "required" to emptyList<String>()
                    )
                )
                "github.search_prs" -> McpToolDefinition(
                    name = name,
                    description = "Looks up pull requests or related GitHub activity for a query.",
                    inputSchema = mapOf(
                        "type" to "object",
                        "properties" to mapOf(
                            "query" to mapOf(
                                "type" to "string",
                                "description" to "Search query or person name."
                            )
                        ),
                        "required" to listOf("query")
                    )
                )
                "system.status" -> McpToolDefinition(
                    name = name,
                    description = "Returns the current service status summary.",
                    inputSchema = mapOf(
                        "type" to "object",
                        "properties" to emptyMap<String, Any>(),
                        "required" to emptyList<String>()
                    )
                )
                "election.search" -> McpToolDefinition(
                    name = name,
                    description = "Searches for public information about Brazilian elections (summary) using a web source.",
                    inputSchema = mapOf(
                        "type" to "object",
                        "properties" to mapOf(
                            "query" to mapOf(
                                "type" to "string",
                                "description" to "Search phrase, e.g. 'Eleições no Brasil 2022' or a candidate/state name."
                            ),
                            "year" to mapOf(
                                "type" to "string",
                                "description" to "Optional year to narrow the search."
                            )
                        ),
                        "required" to emptyList<String>()
                    )
                )
                else -> McpToolDefinition(
                    name = name,
                    description = "Executes a custom MCP tool.",
                    inputSchema = mapOf(
                        "type" to "object",
                        "properties" to emptyMap<String, Any>(),
                        "required" to emptyList<String>()
                    )
                )
            }
        }
    }

    fun get(name: String): McpToolHandler? = tools[name]
}
