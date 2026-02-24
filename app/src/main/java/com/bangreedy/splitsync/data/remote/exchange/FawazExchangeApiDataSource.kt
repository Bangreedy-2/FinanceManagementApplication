package com.bangreedy.splitsync.data.remote.exchange

import android.util.Log
import org.json.JSONObject
import java.io.IOException
import java.net.URL
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Implementation of FxRemoteDataSource using Fawaz Ahmed's currency-api.
 * Handles both jsDelivr CDN and Cloudflare fallback hosts.
 *
 * API Reference: https://github.com/fawazahmed0/currency-api
 */
class FawazExchangeApiDataSource : FxRemoteDataSource {

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE // YYYY-MM-DD
    private val tag = "FawazExchangeApi"

    // API endpoints
    private val jsdelivrBaseUrl = "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api"

    override suspend fun getRatesForBase(base: String, date: LocalDate?): RemoteBaseRates {
        val dateStr = date?.format(dateFormatter) ?: "latest"
        val baseLower = base.lowercase()

        // Try primary endpoint (jsDelivr)
        return try {
            fetchFromPrimary(baseLower, dateStr)
        } catch (e: Exception) {
            Log.w(tag, "jsDelivr endpoint failed for $baseLower/$dateStr, trying fallback", e)
            try {
                fetchFromFallback(baseLower, dateStr)
            } catch (fallbackError: Exception) {
                Log.e(tag, "Both endpoints failed for $baseLower/$dateStr", fallbackError)
                throw fallbackError
            }
        }
    }

    override suspend fun listCurrencies(date: LocalDate?): Map<String, String> {
        val dateStr = date?.format(dateFormatter) ?: "latest"

        return try {
            fetchCurrenciesFromPrimary(dateStr)
        } catch (e: Exception) {
            Log.w(tag, "jsDelivr currencies endpoint failed, trying fallback", e)
            try {
                fetchCurrenciesFromFallback(dateStr)
            } catch (fallbackError: Exception) {
                Log.e(tag, "Both endpoints failed for currencies list", fallbackError)
                throw fallbackError
            }
        }
    }

    private suspend fun fetchFromPrimary(base: String, date: String): RemoteBaseRates {
        // Primary: https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/usd.json
        val url = "$jsdelivrBaseUrl@$date/v1/currencies/$base.json"
        return fetch(url, base, date)
    }

    private suspend fun fetchFromFallback(base: String, date: String): RemoteBaseRates {
        // Fallback: https://latest.currency-api.pages.dev/v1/currencies/usd.json
        val url = "https://$date.currency-api.pages.dev/v1/currencies/$base.json"
        return fetch(url, base, date)
    }

    private suspend fun fetchCurrenciesFromPrimary(date: String): Map<String, String> {
        val url = "$jsdelivrBaseUrl@$date/v1/currencies.json"
        return fetchCurrencies(url)
    }

    private suspend fun fetchCurrenciesFromFallback(date: String): Map<String, String> {
        val url = "https://$date.currency-api.pages.dev/v1/currencies.json"
        return fetchCurrencies(url)
    }

    private suspend fun fetch(url: String, base: String, dateStr: String): RemoteBaseRates {
        Log.d(tag, "Fetching from $url")
        val response = URL(url).readText()
        val json = JSONObject(response)

        // Response structure: { "date": "2024-03-06", "eur": { "usd": 1.09, ... } }
        val apiDate = json.optString("date", dateStr)
        val baseRatesObj = json.optJSONObject(base)
            ?: throw IOException("No rates found for currency $base in response")

        val rates = mutableMapOf<String, Double>()
        val keys = baseRatesObj.keys()
        while (keys.hasNext()) {
            val quote = keys.next()
            val rate = baseRatesObj.getDouble(quote)
            rates[quote.lowercase()] = rate
        }

        val date = try {
            LocalDate.parse(apiDate, dateFormatter)
        } catch (e: Exception) {
            Log.w(tag, "Failed to parse date $apiDate, using provided date", e)
            if (dateStr == "latest") LocalDate.now() else LocalDate.parse(dateStr, dateFormatter)
        }

        return RemoteBaseRates(
            base = base.uppercase(),
            date = date,
            rates = rates
        )
    }

    private suspend fun fetchCurrencies(url: String): Map<String, String> {
        Log.d(tag, "Fetching currencies from $url")
        val response = URL(url).readText()
        val json = JSONObject(response)

        val currencies = mutableMapOf<String, String>()
        val keys = json.keys()
        while (keys.hasNext()) {
            val code = keys.next()
            if (code != "date") {
                val name = json.getString(code)
                currencies[code.uppercase()] = name
            }
        }

        return currencies
    }
}


