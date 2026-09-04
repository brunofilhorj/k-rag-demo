package com.bfilho.kragdemo.adapter.out.mcp.tool

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class YouComSearchResponse(
    val results: List<YouComItem>? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class YouComItem(
    val title: String? = null,
    val url: String? = null,
    val snippet: String? = null,
    val highlights: List<String>? = null,
    val content: String? = null
)
