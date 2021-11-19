package net.pfiers.osmfocus.service.util

import android.net.Uri

private val BASE_ISSUE_URL = Uri.parse("https://github.com/ubipo/osmfocus/issues/new")

fun createGitHubIssueUrl(
    title: String,
    body: String,
    labels: List<String>,
    assignees: List<String>
): Uri = BASE_ISSUE_URL.buildUpon()
    .appendQueryParameter("title", title)
    .appendQueryParameter("body", body)
    .appendQueryParameter("labels", labels.joinToString(","))
    .appendQueryParameter("assignees", assignees.joinToString(","))
    .build()
