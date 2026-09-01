package org.bigblackowl.debttracker.domain.repository

import org.bigblackowl.debttracker.domain.model.ExchangeRatesSnapshot
import org.bigblackowl.debttracker.domain.model.RateSource

/**
 * Курси валют для [org.bigblackowl.debttracker.ui.screens.exchange.ExchangeRatesScreen].
 * Онлайн-only (як і решта remote-репозиторіїв), але останній вдалий зріз кожної пари
 * (джерело, база) кешується локально, щоб екран щось показував і без мережі.
 * Для банків [baseCode] ігнорується (база фіксована), для [RateSource.arbitraryBase]-джерел
 * це обрана користувачем валюта котирування.
 */
interface ExchangeRatesRepository {

    /** Останній збережений зріз для пари, якщо він колись вантажився на цьому пристрої. */
    fun cached(source: RateSource, baseCode: String): ExchangeRatesSnapshot?

    /** Тягне свіжі курси з мережі й оновлює кеш. Кидає при помилці мережі/розбору відповіді. */
    suspend fun refresh(source: RateSource, baseCode: String): ExchangeRatesSnapshot
}
