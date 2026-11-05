package si.um.feri.maprri.raster.enums

import java.net.URL

enum class MapServiceVendor(
    val mapServiceURL: String,
    val tilesetID: String,
    val format: String,
    val apiKey: String
) {
    GEOAPIFY(
        mapServiceURL = "https://maps.geoapify.com/v1/tile/",
        tilesetID = "klokantech-basic",
        format = "@2x.png",
        apiKey = ""
    ) {
        override val token: String get() = "?&apiKey=$apiKey"
    },
    MAPBOX(
        mapServiceURL = "https://api.mapbox.com/v4/",
        tilesetID = "mapbox.satellite",
        format = "@2x.jpg90",
        apiKey = "",
    ) {
        override val token: String get() = "?access_token=$apiKey"
    };

    abstract val token: String

    fun buildURL(path: String): URL = URL("${mapServiceURL}${tilesetID}/$path${format}${token}")
    fun buildURL(tilesetID: String, path: String): URL = URL("${mapServiceURL}${tilesetID}/$path${format}${token}")
}
