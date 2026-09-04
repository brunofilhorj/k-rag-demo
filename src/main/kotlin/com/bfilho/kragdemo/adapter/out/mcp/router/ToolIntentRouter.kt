package com.bfilho.kragdemo.adapter.out.mcp.router

import com.bfilho.kragdemo.adapter.out.mcp.contract.McpToolDecision
import org.springframework.stereotype.Component

@Component
class ToolIntentRouter {

    fun route(question: String): List<McpToolDecision> {
        val trimmed = question.trim()
        if (trimmed.isBlank()) {
            return emptyList()
        }

        val decisions = mutableListOf<McpToolDecision>()

        val jiraDecision = decideJiraDecision(trimmed)
        if (jiraDecision != null) {
            decisions += jiraDecision
        }

        if (shouldInvokeGithubSearch(trimmed)) {
            decisions += McpToolDecision(
                toolName = "github.search_prs",
                arguments = mapOf("query" to extractGithubQuery(trimmed))
            )
        }

        val internetDecision = decideInternetSearch(trimmed)
        if (internetDecision != null) {
            decisions += internetDecision
        }

        if (shouldInvokeSystemStatus(trimmed)) {
            decisions += McpToolDecision(
                toolName = "system.status",
                arguments = emptyMap()
            )
        }

        return decisions
    }

    private fun decideJiraDecision(question: String): McpToolDecision? {
        val normalized = question.lowercase()
        val explicitKey = Regex("(?:issue|ticket|bug|task)[\\s]*[kK]ey[\\s]*[:=][\\s]*([A-Za-z]+[-_.]?[A-Za-z0-9]+)", RegexOption.IGNORE_CASE)
            .find(question)?.groupValues?.getOrNull(1)
        if (explicitKey != null) {
            return McpToolDecision(
                toolName = "jira.get_issue",
                arguments = mapOf("issueKey" to explicitKey)
            )
        }

        val uppercaseKey = Regex("\\b[A-Z]+-\\d+\\b").find(question)?.value
        if (uppercaseKey != null) {
            return McpToolDecision(
                toolName = "jira.get_issue",
                arguments = mapOf("issueKey" to uppercaseKey)
            )
        }

        val lowercaseKey = Regex("\\b[a-z]+[-_.]?[0-9]+\\b", RegexOption.IGNORE_CASE).find(question)?.value
        if (lowercaseKey != null) {
            return McpToolDecision(
                toolName = "jira.get_issue",
                arguments = mapOf("issueKey" to lowercaseKey)
            )
        }

        val issueKeywords = listOf("issue", "issues", "ticket", "tickets", "bug", "bugs", "task", "tasks")
        val asksForIssueList = (normalized.contains("todas as issues") || normalized.contains("all issues") || normalized.contains("issues abertas") || normalized.contains("issues fechadas") || normalized.contains("busque todas as issues") || normalized.contains("buscar issues") || normalized.contains("pesquise issues") || normalized.contains("procure issues"))
        val hasIssueContext = issueKeywords.any { normalized.contains(it) }

        if (hasIssueContext || asksForIssueList) {
            val assignee = extractAssignee(question)
            val status = extractIssueStatus(question)
            return McpToolDecision(
                toolName = "jira.list_issues",
                arguments = mapOf(
                    "assignee" to (assignee ?: "all"),
                    "status" to (status ?: "all")
                )
            )
        }

        return null
    }

    private fun extractAssignee(question: String): String? {
        val names = listOf("bruno", "joao", "joão", "maria", "ana", "lucas")
        return names.firstOrNull { question.lowercase().contains(it) }
    }

    private fun extractIssueStatus(question: String): String? {
        val normalized = question.lowercase()
        return when {
            normalized.contains("aberta") || normalized.contains("open") || normalized.contains("opened") -> "OPEN"
            normalized.contains("fechada") || normalized.contains("closed") || normalized.contains("closeds") -> "CLOSED"
            normalized.contains("review") || normalized.contains("in review") -> "IN_REVIEW"
            else -> null
        }
    }

    private fun shouldInvokeGithubSearch(question: String): Boolean {
        val normalized = question.lowercase()
        val githubPatterns = listOf("pr", "pull request", "pull-request", "branch", "commit", "release", "merge")
        return githubPatterns.any { normalized.contains(it) }
    }

    private fun decideInternetSearch(question: String): McpToolDecision? {
        val normalized = question.lowercase()
        val searchVerbs = listOf("busca", "buscar", "busque", "pesquise", "pesquisar", "procure", "encontre")
        val hasSearch = searchVerbs.any { normalized.contains(it) }
        if (!hasSearch) return null

        val targets = mutableListOf<String>()
        if (normalized.contains("google") || normalized.contains("web")) targets += "web"
        if (normalized.contains("wikipedia") || normalized.contains("wiki")) targets += "wikipedia"
        if (normalized.contains("site") || normalized.contains("site:")) targets += "url"

        if (targets.isEmpty()) targets += "wikipedia"

        val query = extractSearchQuery(question)

        return McpToolDecision(
            toolName = "election.search",
            arguments = mapOf(
                "query" to query,
                "targets" to targets
            )
        )
    }

    private fun extractSearchQuery(question: String): String {
        val lowered = question.lowercase()
        val cleaned = lowered
            .replace(Regex("(busca|buscar|busque|pesquise|pesquisar|procure|encontre)"), "")
            .replace(Regex("(google|wikipedia|wiki|site|site:)"), "")
            .replace(Regex("[^a-z0-9\\sçãõáàéíóú-]"), " ")
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .joinToString(" ")
        return if (cleaned.isBlank()) question.trim() else cleaned.trim()
    }

    private fun shouldInvokeSystemStatus(question: String): Boolean {
        val normalized = question.lowercase()
        return normalized.contains("status") || normalized.contains("saúde") || normalized.contains("health")
    }

    private fun extractGithubQuery(question: String): String {
        val words = question.lowercase().split(Regex("[^a-z0-9]+"))
            .filter { it.isNotBlank() }
        val filtered = words.filterNot { it in setOf(
            "tem", "algum", "alguma", "existe", "ha", "há", "para", "qual", "quais", "com", "sobre",
            "do", "da", "de", "em", "o", "a", "e", "ou", "alguns", "algumas"
        ) }
        return filtered.joinToString(" ").ifBlank { question.trim() }
    }
}
