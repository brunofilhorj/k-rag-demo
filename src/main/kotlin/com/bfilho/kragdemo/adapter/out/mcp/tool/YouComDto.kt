package com.bfilho.kragdemo.adapter.out.mcp.tool

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class YouComSearchResponse(
    val results: Map<String, List<YouComRawItem>>? = null,
    val metadata: YouComMetadata? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class YouComRawItem(
    val url: String? = null,
    val title: String? = null,
    val description: String? = null,
    val thumbnail_url: String? = null,
    val original_thumbnail_url: String? = null,
    val page_age: String? = null,
    val favicon_url: String? = null,
    val contents: YouComContents? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class YouComContents(
    val highlights: List<String>? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class YouComMetadata(
    val query: String? = null,
    val search_uuid: String? = null,
    val latency: Double? = null
)
