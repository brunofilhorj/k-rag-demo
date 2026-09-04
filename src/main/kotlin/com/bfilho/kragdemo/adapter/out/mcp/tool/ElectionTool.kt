package com.bfilho.kragdemo.adapter.out.mcp.tool

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class ElectionTool(
    private val internetSearchService: InternetSearchService
) : McpToolHandler {

    private val log = LoggerFactory.getLogger(ElectionTool::class.java)

    override fun invoke(arguments: Map<String, Any>): String {
        val query = (arguments["query"] as? String)?.takeIf { it.isNotBlank() }
            ?: arguments["year"]?.toString()?.let { "Eleições no Brasil $it" }
            ?: "Eleições no Brasil"

        // allow optional targets argument: list or comma-separated string
        val targetsArg = arguments["targets"]
        val targets: List<String>? = when (targetsArg) {
            is List<*> -> targetsArg.filterIsInstance<String>().ifEmpty { null }
            is String -> targetsArg.split(',').map { it.trim() }.filter { it.isNotBlank() }.ifEmpty { null }
            else -> null
        }

        return try {
            val results = internetSearchService.fetchFromTargets(query, targets)
            // prefer first successful target result
            val firstNonNull = results.values.firstOrNull { !it.isNullOrBlank() }
            if (!firstNonNull.isNullOrBlank()) {
                // include a short header mentioning which targets were used
                val usedTargets = results.keys.joinToString(", ")
                "[Searched targets: $usedTargets]\n\n$firstNonNull"
            } else {
                "No info found for '$query' across targets: ${results.keys.joinToString(", ")}."
            }
        } catch (e: Exception) {
            log.warn("ElectionTool fetch failed: {}", e.message)
            "Could not fetch election info for '$query': ${'$'}{e.message}"
        }
    }
}
