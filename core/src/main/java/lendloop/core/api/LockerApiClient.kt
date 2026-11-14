package lendloop.core.api

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Net
import com.badlogic.gdx.net.HttpRequestBuilder
import com.badlogic.gdx.utils.JsonReader
import com.badlogic.gdx.utils.JsonValue
import lendloop.core.locker.LockerBox
import lendloop.core.locker.LockerStation

class LockerApiClient(private val baseUrl: String) {

    fun fetchStations(
        availableOnly: Boolean = false,
        onSuccess: (List<LockerStation>) -> Unit,
        onError: (Throwable) -> Unit = { Gdx.app.error("LockerApi", "fetchStations", it) }
    ) {
        val url = "$baseUrl/locker-stations" + if (availableOnly) "?available_only=true" else ""
        send(url,
            onSuccess = { body -> onSuccess(parseStations(body)) },
            onError = onError
        )
    }

    fun fetchDetail(
        stationId: String,
        onSuccess: (List<LockerBox>) -> Unit,
        onError: (Throwable) -> Unit = { Gdx.app.error("LockerApi", "fetchDetail", it) }
    ) {
        val url = "$baseUrl/locker-stations/$stationId"
        send(url,
            onSuccess = { body -> onSuccess(parseLockerBoxes(body)) },
            onError = onError
        )
    }

    private fun send(
        url: String,
        onSuccess: (String) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val request = HttpRequestBuilder()
            .newRequest()
            .url(url)
            .method(Net.HttpMethods.GET)
            .build()

        Gdx.net.sendHttpRequest(request, object : Net.HttpResponseListener {
            override fun handleHttpResponse(response: Net.HttpResponse) {
                val body = response.resultAsString
                val status = response.status.statusCode
                Gdx.app.postRunnable {
                    if (status in 200..299) {
                        runCatching { onSuccess(body) }.onFailure(onError)
                    } else {
                        onError(RuntimeException("HTTP $status: $body"))
                    }
                }
            }
            override fun failed(t: Throwable) {
                Gdx.app.postRunnable { onError(t) }
            }
            override fun cancelled() { /* nothing */ }
        })
    }

    private fun parseStations(json: String): List<LockerStation> {
        val root = JsonReader().parse(json)
        val out = ArrayList<LockerStation>(root.size)
        var v: JsonValue? = root.child
        while (v != null) {
            val loc = v.get("location")
            val wt  = v.get("working_times")
            out += LockerStation(
                id              = v.getString("id"),
                stationName     = v.getString("station_name"),
                address         = v.getString("address"),
                latitude        = loc.getFloat("latitude").toDouble(),
                longitude       = loc.getFloat("longitude").toDouble(),
                lockerCount     = v.getInt("locker_count"),
                availableCount  = v.getInt("available_count"),
                workingTimesJson = wt?.toString() ?: "{}"
            )
            v = v.next
        }
        return out
    }

    private fun parseLockerBoxes(json: String): List<LockerBox> {
        val root = JsonReader().parse(json)
        val arr = root.get("lockers") ?: return emptyList()
        val out = ArrayList<LockerBox>(arr.size)
        var v: JsonValue? = arr.child
        while (v != null) {
            out += LockerBox(
                id        = v.getString("id"),
                boxNumber = v.getInt("box_number").toShort(),
                maxWeight = v.getFloat("max_weight").toDouble(),
                available = v.getBoolean("available")
            )
            v = v.next
        }
        return out
    }
}
