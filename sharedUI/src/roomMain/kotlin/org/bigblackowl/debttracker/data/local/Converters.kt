package org.bigblackowl.debttracker.data.local

import androidx.room.TypeConverter
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.datetime.Instant

/** Room [androidx.room.TypeConverter]s for the two types entities need that SQLite has no native column type for. */
class Converters {
    @TypeConverter
    fun fromInstant(value: Instant): Long = value.toEpochMilliseconds()

    @TypeConverter
    fun toInstant(value: Long): Instant = Instant.fromEpochMilliseconds(value)

    @TypeConverter
    fun fromBigDecimal(value: BigDecimal): String = value.toStringExpanded()

    @TypeConverter
    fun toBigDecimal(value: String): BigDecimal = BigDecimal.parseString(value)
}
