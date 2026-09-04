package com.bfilho.kragdemo.adapter.out.mcp.tool

import org.springframework.stereotype.Component

@Component
class JiraIssueTool : McpToolHandler {

    private val issues = listOf(
        IssueRecord("ABC-100", "Bruno", "OPEN", "Login page crash on Safari"),
        IssueRecord("ABC-101", "Joao", "CLOSED", "Add retry logic for API client"),
        IssueRecord("ABC-102", "Bruno", "OPEN", "Improve JWT refresh flow"),
        IssueRecord("ABC-103", "Maria", "IN_REVIEW", "Fix permissions matrix"),
        IssueRecord("ABC-104", "Bruno", "CLOSED", "Document MCP connector contract"),
        IssueRecord("ABC-105", "Lucas", "OPEN", "Improve dashboard cache invalidation"),
        IssueRecord("ABC-106", "Bruno", "IN_REVIEW", "Refactor RAG prompt assembly"),
        IssueRecord("ABC-107", "Joao", "OPEN", "Fix CSV export timeout"),
        IssueRecord("ABC-108", "Ana", "CLOSED", "Optimize ingestion batch size"),
        IssueRecord("ABC-109", "Bruno", "OPEN", "Review deployment rollback checklist")
    )

    override fun invoke(arguments: Map<String, Any>): String {
        val issueKey = arguments["issueKey"] as? String ?: return "No issue key was provided."
        val found = issues.firstOrNull { it.key.equals(issueKey, ignoreCase = true) }
            ?: return "No issue found for key '$issueKey'."

        return "Issue ${found.key}: ${found.status} | owner=${found.owner} | status=${found.status} | summary=${found.summary}"
    }

    fun listIssues(assignee: String? = null, status: String? = null): List<IssueRecord> {
        val assigneeFilter = assignee?.takeUnless { it.equals("all", ignoreCase = true) }
        val statusFilter = status?.takeUnless { it.equals("all", ignoreCase = true) }

        return issues.filter { issue ->
            (assigneeFilter == null || issue.owner.equals(assigneeFilter, ignoreCase = true)) &&
                (statusFilter == null || issue.status.equals(statusFilter, ignoreCase = true))
        }
    }
}

data class IssueRecord(
    val key: String,
    val owner: String,
    val status: String,
    val summary: String
)
