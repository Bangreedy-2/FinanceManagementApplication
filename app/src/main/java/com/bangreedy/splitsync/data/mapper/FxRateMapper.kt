package com.bangreedy.splitsync.data.mapper

import com.bangreedy.splitsync.data.local.entity.FxRateEntity
import com.bangreedy.splitsync.domain.model.FxRate
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

fun FxRateEntity.toDomain(): FxRate =
    FxRate(
        base = base,
        quote = quote,
        rateDate = LocalDate.parse(rateDate, dateFormatter),
        rate = rate,
        fetchedAt = Instant.ofEpochMilli(fetchedAt)
    )

fun FxRate.toEntity(): FxRateEntity {
    val dateStr = rateDate.format(dateFormatter)
    return FxRateEntity(
        id = "${base}_${quote}_$dateStr",
        base = base,
        quote = quote,
        rate = rate,
        rateDate = dateStr,
        fetchedAt = fetchedAt.toEpochMilli()
    )
}

