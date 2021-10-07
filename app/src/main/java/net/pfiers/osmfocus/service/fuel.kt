package net.pfiers.osmfocus.service

import com.github.kittinunf.fuel.httpGet
import java.net.URI

fun URI.httpGet() = toString().httpGet()
