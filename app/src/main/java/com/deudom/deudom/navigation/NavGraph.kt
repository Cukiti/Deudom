package com.deudom.deudom.navigation

sealed class Screen(val route: String) {
    object Auth : Screen("auth")
    object Tutorial : Screen("tutorial")
    object Home : Screen("home")
    object Lend : Screen("lend")
    object LoanDetail : Screen("loan_detail/{loanId}") {
        fun createRoute(loanId: String) = "loan_detail/$loanId"
    }
}
