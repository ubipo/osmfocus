package net.pfiers.osmfocus.service.util

import com.github.kittinunf.fuel.httpGet
import java.net.URI

fun URI.httpGet() = toString().httpGet()
