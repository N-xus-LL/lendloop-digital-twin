package lendloop

import java.net.URL

val isDebug = false

fun URL.readBytes(): ByteArray = openStream().use { it.readBytes() }
