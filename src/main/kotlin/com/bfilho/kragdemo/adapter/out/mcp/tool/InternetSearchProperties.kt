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
}
