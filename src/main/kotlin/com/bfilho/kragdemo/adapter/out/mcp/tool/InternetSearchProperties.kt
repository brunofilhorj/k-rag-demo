package com.bfilho.kragdemo.adapter.out.mcp.tool

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "internet.search")
class InternetSearchProperties {
    var ttlMs: Long = 10 * 60 * 1000L
    var defaultTargets: List<String> = listOf("wikipedia")
    // default language for searches (used for wikipedia, etc.)
    var defaultLanguage: String = "pt"

    // User-Agent header to use when scraping web targets
    var userAgent: String = "k-rag-demo/1.0 (+https://github.com/brunofilhorj/k-rag-demo)"

    // You.com configuration
    var youcom: YouComProperties = YouComProperties()

    data class YouComProperties(
        var apiKey: String = "",
        var maxResults: Int = 5,
        var language: String = "pt-BR",
        var country: String = "BR"
    )
}

