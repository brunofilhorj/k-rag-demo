package com.bfilho.kragdemo.adapter.out.mcp.tool

import org.springframework.stereotype.Component

@Component
class JiraIssueListTool(
    private val jiraIssueTool: JiraIssueTool
) : McpToolHandler {

    override fun invoke(arguments: Map<String, Any>): String {
        val assignee = arguments["assignee"] as? String
        val status = arguments["status"] as? String
        val issues = jiraIssueTool.listIssues(assignee, status)

        if (issues.isEmpty()) {
            val assigneeLabel = assignee ?: "the provided assignee"
            val statusLabel = status ?: "all"
            return "No Jira issues were found for $assigneeLabel with status '$statusLabel'."
        }

        return issues.joinToString("\n") { issue ->
            "${issue.key} | owner=${issue.owner} | status=${issue.status} | summary=${issue.summary}"
        }
    }
}
