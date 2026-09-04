package com.bfilho.kragdemo.adapter.out.mcp

import com.bfilho.kragdemo.adapter.out.mcp.tool.GithubSearchPrsTool
import com.bfilho.kragdemo.adapter.out.mcp.tool.JiraIssueListTool
import com.bfilho.kragdemo.adapter.out.mcp.tool.JiraIssueTool
import com.bfilho.kragdemo.adapter.out.mcp.tool.SystemStatusTool
import com.bfilho.kragdemo.adapter.out.mcp.tool.ElectionTool
import org.springframework.stereotype.Component

@Component
class McpToolBootstrap(
    toolRegistry: McpToolRegistry,
    jiraIssueTool: JiraIssueTool,
    jiraIssueListTool: JiraIssueListTool,
    systemStatusTool: SystemStatusTool,
    githubSearchPrsTool: GithubSearchPrsTool,
    electionTool: ElectionTool
) {
    init {
        toolRegistry.register("jira.get_issue", jiraIssueTool)
        toolRegistry.register("jira.list_issues", jiraIssueListTool)
        toolRegistry.register("system.status", systemStatusTool)
        toolRegistry.register("github.search_prs", githubSearchPrsTool)
        toolRegistry.register("election.search", electionTool)
    }
}
