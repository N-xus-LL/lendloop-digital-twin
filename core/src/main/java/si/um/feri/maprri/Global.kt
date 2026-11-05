package si.um.feri.maprri

import java.net.URL

val isDebug = false

fun URL.readBytes(): ByteArray = openStream().use { it.readBytes() }
