package org.bigblackowl.debttracker.domain.repository

import org.bigblackowl.debttracker.domain.model.ExchangeRatesSnapshot
import org.bigblackowl.debttracker.domain.model.RateSource

/**
 * Exchange rates for [org.bigblackowl.debttracker.ui.screens.exchange.ExchangeRatesScreen].
 * Online-only (like the rest of the remote repositories), but the last successful snapshot of each
 * (source, base) pair is cached locally so the screen shows something even without a network.
 * For banks [baseCode] is ignored (the base is fixed); for [RateSource.arbitraryBase] sources
 * it's the quoting currency chosen by the user.
 */
interface ExchangeRatesRepository {

    /** The last saved snapshot for the pair, if it was ever loaded on this device. */
    fun cached(source: RateSource, baseCode: String): ExchangeRatesSnapshot?

    /** Fetches fresh rates from the network and updates the cache. Throws on a network/parse error. */
    suspend fun refresh(source: RateSource, baseCode: String): ExchangeRatesSnapshot
}
