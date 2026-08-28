package org.bigblackowl.debttracker.domain.usecase.debtor

import org.bigblackowl.debttracker.domain.repository.DebtorRepository

/**
 * Прив'язує щойно збереженого боржника до зареєстрованого користувача за
 * телефоном/email, якщо збіг знайдеться (RPC `link_debtor_to_registered_user`,
 * ідемпотентна) — дзеркалить наявні транзакції в акаунт того користувача й
 * надсилає йому сповіщення. Fire-and-forget з боку викликача: помилка/відсутність
 * збігу ніколи не повинна блокувати збереження боржника.
 */
class LinkDebtorToRegisteredUserUseCase(private val repository: DebtorRepository) {
    suspend operator fun invoke(debtorId: String): String? = repository.linkToRegisteredUser(debtorId)
}
