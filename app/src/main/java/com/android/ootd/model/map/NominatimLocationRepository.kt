package com.android.ootd.model.map

import android.util.Log
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray

/**
 * Implementation of [LocationRepository] that queries the OpenStreetMap Nominatim API.
 *
 * Uses an injected [OkHttpClient] to perform network calls and parses the JSON array response into
 * a list of [Location]. Network calls are dispatched on [kotlinx.coroutines.Dispatchers.IO].
 *
 * Note: This file is taken from the Bootcamp Week 3 solution.
 */
class NominatimLocationRepository(private val client: OkHttpClient) : LocationRepository {

  private fun parseBody(body: String): List<Location> {
    val jsonArray = JSONArray(body)

    return List(jsonArray.length()) { i ->
      val jsonObject = jsonArray.getJSONObject(i)
      val lat = jsonObject.getDouble("lat")
      val lon = jsonObject.getDouble("lon")
      val name = jsonObject.getString("display_name")
      Location(lat, lon, name)
    }
  }

  override suspend fun search(query: String): List<Location> =
      withContext(Dispatchers.IO) {
        // Using HttpUrl.Builder to properly construct the URL with query parameters.
        val url =
            HttpUrl.Builder()
                .scheme("https")
                .host("nominatim.openstreetmap.org")
                .addPathSegment("search")
                .addQueryParameter("q", query)
                .addQueryParameter("format", "json")
                .build()

        // Create the request with a custom User-Agent
        val request =
            Request.Builder()
                .url(url)
                .header("User-Agent", "Outfit Of The Day (ooftheday56@gmail.com)")
                .build()

        try {
          val response = client.newCall(request).execute()
          response.use {
            if (!response.isSuccessful) {
              Log.d("NominatimLocationRepository", "Unexpected code $response")
              throw Exception("Unexpected code $response")
            }

            val body = response.body?.string()
            val result =
                if (body != null) {
                  Log.d("NominatimLocationRepository", "Body: $body")
                  parseBody(body)
                } else {
                  Log.d("NominatimLocationRepository", "Empty body")
                  emptyList()
                }

            return@withContext result
          }
        } catch (e: IOException) {
          Log.e("NominatimLocationRepository", "Failed to execute request", e)
          throw e
        }
      }
}
