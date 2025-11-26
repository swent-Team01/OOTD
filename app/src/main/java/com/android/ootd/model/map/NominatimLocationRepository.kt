package com.android.ootd.model.map

import android.util.Log
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

/**
 * Implementation of [LocationRepository] that queries the OpenStreetMap Nominatim API.
 *
 * Uses an injected [OkHttpClient] to perform network calls and parses the JSON array response into
 * a list of [Location]. Network calls are dispatched on [kotlinx.coroutines.Dispatchers.IO].
 *
 * Note: This file is taken from the Bootcamp Week 3 solution.
 */
class NominatimLocationRepository(private val client: OkHttpClient) : LocationRepository {

  companion object {
    private const val TAG = "NominatimLocationRepository"
    private const val BASE_URL = "nominatim.openstreetmap.org"
    private const val USER_AGENT = "Outfit Of The Day (ooftheday56@gmail.com)"
  }

  private fun parseBody(body: String): List<Location> {
    val jsonArray = JSONArray(body)

    return List(jsonArray.length()) { i -> parseLocationFromJson(jsonArray.getJSONObject(i)) }
  }

  private fun parseLocationFromJson(jsonObject: JSONObject): Location {
    val lat = jsonObject.getDouble("lat")
    val lon = jsonObject.getDouble("lon")
    val name = jsonObject.getString("display_name")
    return Location(lat, lon, name)
  }

  private suspend fun executeRequest(url: HttpUrl): String? =
      withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()

        try {
          val response = client.newCall(request).execute()
          response.use {
            if (!response.isSuccessful) {
              throw Exception("Unexpected code $response")
            }
            response.body?.string()
          }
        } catch (e: IOException) {
          Log.e(TAG, "Failed to execute request", e)
          throw e
        }
      }

  override suspend fun search(query: String): List<Location> {
    // Using HttpUrl.Builder to properly construct the URL with query parameters.
    val url =
        HttpUrl.Builder()
            .scheme("https")
            .host(BASE_URL)
            .addPathSegment("search")
            .addQueryParameter("q", query)
            .addQueryParameter("format", "json")
            .build()

    try {
      val body = executeRequest(url)
      return if (body != null) {
        parseBody(body)
      } else {
        emptyList()
      }
    } catch (e: IOException) {
      Log.e("NominatimLocationRepository", "Failed to execute request", e)
      throw e
    }
  }

  override suspend fun reverseGeocode(latitude: Double, longitude: Double): Location {
    val url =
        HttpUrl.Builder()
            .scheme("https")
            .host(BASE_URL)
            .addPathSegment("reverse")
            .addQueryParameter("lat", latitude.toString())
            .addQueryParameter("lon", longitude.toString())
            .addQueryParameter("format", "json")
            .build()

    val body = executeRequest(url)
    return if (body != null) {
      parseLocationFromJson(JSONObject(body))
    } else {
      throw Exception("Empty response from reverse geocoding")
    }
  }
}
