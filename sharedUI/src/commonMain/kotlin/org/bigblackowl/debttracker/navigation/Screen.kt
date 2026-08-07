package org.bigblackowl.debttracker.navigation

/** Типізовані маршрути навігаційного графа (спек §6). */
sealed interface Screen {
    data object Splash : Screen
    data object AuthGate : Screen
    data object Home : Screen
    data class AddEditDebtor(val debtorId: String? = null) : Screen
    data class DebtorDetail(val debtorId: String) : Screen
    data class AddEditCreditor(val creditorId: String? = null) : Screen
    data class CreditorDetail(val creditorId: String) : Screen
    data object Stats : Screen
    data object Settings : Screen
    data object Export : Screen
    data class Auth(val isGate: Boolean = false) : Screen
}
