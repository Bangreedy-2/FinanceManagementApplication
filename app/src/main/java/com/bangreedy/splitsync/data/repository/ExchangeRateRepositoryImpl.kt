package com.bangreedy.splitsync.data.repository

import android.util.Log
import com.bangreedy.splitsync.data.local.dao.FxRateDao
import com.bangreedy.splitsync.data.mapper.toDomain
import com.bangreedy.splitsync.data.mapper.toEntity
import com.bangreedy.splitsync.data.remote.exchange.FxRemoteDataSource
import com.bangreedy.splitsync.domain.model.FxRate
import com.bangreedy.splitsync.domain.model.FxRateResult
import com.bangreedy.splitsync.domain.repository.ExchangeRateRepository
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * ExchangeRateRepository implementation with cache-first strategy.
 *
 * Cache policy:
 * - Historical dates: fetch once from remote, cache forever (immutable)
 * - Latest/today: use cache if fresh (< 6 hours), else fetch remote
 * - If remote fails and cache exists: return cached rate (may be stale)
 * - If no cache: return error
 */
class ExchangeRateRepositoryImpl(
    private val fxDao: FxRateDao,
    private val remoteDataSource: FxRemoteDataSource
) : ExchangeRateRepository {

    private val tag = "ExchangeRateRepository"
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val ttlMillis = 6 * 60 * 60 * 1000 // 6 hours

    override suspend fun getRate(
        base: String,
        quote: String,
        date: LocalDate?
    ): FxRateResult {
        val targetDate = date ?: LocalDate.now()
        val dateStr = targetDate.format(dateFormatter)
        val baseLower = base.lowercase()
        val quoteLower = quote.lowercase()

        // Try cache first
        val cached = fxDao.getRate(baseLower, quoteLower, dateStr)
        if (cached != null && isCacheFresh(cached, targetDate)) {
            Log.d(tag, "Cache hit for $baseLower->$quoteLower on $dateStr")
            return FxRateResult.Success(cached.toDomain())
        }

        // Try remote
        return try {
            val remoteRates = remoteDataSource.getRatesForBase(baseLower, targetDate)
            val rate = remoteRates.rates[quoteLower]
                ?: throw Exception("Quote currency $quoteLower not found in remote rates")

            val fxRate = FxRate(
                base = baseLower,
                quote = quoteLower,
                rateDate = remoteRates.date,
                rate = rate,
                fetchedAt = Instant.now()
            )

            // Store in cache
            fxDao.upsert(fxRate.toEntity())
            Log.d(tag, "Fetched and cached $baseLower->$quoteLower from remote")

            FxRateResult.Success(fxRate)
        } catch (e: Exception) {
            Log.e(tag, "Remote fetch failed for $baseLower->$quoteLower", e)

            // Fallback: try any cached rate for this pair, even if stale
            val staleCached = fxDao.getLatestRateBeforeOrOn(baseLower, quoteLower, dateStr)
            if (staleCached != null) {
                Log.w(tag, "Using stale cached rate for $baseLower->$quoteLower")
                FxRateResult.Success(staleCached.toDomain())
            } else {
                FxRateResult.Error("Conversion unavailable: ${e.message}")
            }
        }
    }

    override suspend fun getRates(
        base: String,
        quotes: Set<String>,
        date: LocalDate?
    ): Map<String, FxRateResult> {
        val targetDate = date ?: LocalDate.now()
        val baseLower = base.lowercase()

        return try {
            val remoteRates = remoteDataSource.getRatesForBase(baseLower, targetDate)

            val results = mutableMapOf<String, FxRateResult>()
            val entitiesToCache = mutableListOf<com.bangreedy.splitsync.data.local.entity.FxRateEntity>()

            for (quote in quotes) {
                val quoteLower = quote.lowercase()
                val rate = remoteRates.rates[quoteLower]

                if (rate != null) {
                    val fxRate = FxRate(
                        base = baseLower,
                        quote = quoteLower,
                        rateDate = remoteRates.date,
                        rate = rate,
                        fetchedAt = Instant.now()
                    )
                    results[quoteLower] = FxRateResult.Success(fxRate)
                    entitiesToCache.add(fxRate.toEntity())
                } else {
                    results[quoteLower] = FxRateResult.Error("Quote $quoteLower not found")
                }
            }

            if (entitiesToCache.isNotEmpty()) {
                fxDao.upsertAll(entitiesToCache)
            }

            Log.d(tag, "Fetched ${entitiesToCache.size} rates for base $baseLower")
            results
        } catch (e: Exception) {
            Log.e(tag, "Remote fetch failed for base $baseLower", e)

            // Fallback to cache for each quote
            val results = mutableMapOf<String, FxRateResult>()
            val dateStr = targetDate.format(dateFormatter)

            for (quote in quotes) {
                val quoteLower = quote.lowercase()
                val stale = fxDao.getLatestRateBeforeOrOn(baseLower, quoteLower, dateStr)

                if (stale != null) {
                    Log.w(tag, "Using stale rate for $baseLower->$quoteLower")
                    results[quoteLower] = FxRateResult.Success(stale.toDomain())
                } else {
                    results[quoteLower] = FxRateResult.Error("Conversion unavailable offline")
                }
            }

            results
        }
    }

    private fun isCacheFresh(entity: com.bangreedy.splitsync.data.local.entity.FxRateEntity, targetDate: LocalDate): Boolean {
        // Historical dates are immutable, always use cache
        if (targetDate < LocalDate.now()) {
            return true
        }

        // For today, check TTL
        val now = System.currentTimeMillis()
        return (now - entity.fetchedAt) < ttlMillis
    }
}

