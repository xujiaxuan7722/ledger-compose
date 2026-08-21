package com.dwt.ledger.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dwt.ledger.ui.addedit.AddEditTransactionScreen
import com.dwt.ledger.ui.budget.BudgetScreen
import com.dwt.ledger.ui.manage.ManageScreen
import com.dwt.ledger.ui.statistics.StatisticsScreen
import com.dwt.ledger.ui.transactions.TransactionsScreen

object LedgerRoutes {
    const val TRANSACTIONS = "transactions"
    const val STATISTICS = "statistics"
    const val BUDGET = "budget"
    const val MANAGE = "manage"
    const val ARG_TRANSACTION_ID = "transactionId"
    const val ADD_EDIT = "addEdit?$ARG_TRANSACTION_ID={$ARG_TRANSACTION_ID}"
    fun addEdit(transactionId: String? = null): String =
        if (transactionId == null) "addEdit" else "addEdit?$ARG_TRANSACTION_ID=$transactionId"
}

@Composable
fun LedgerNavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = LedgerRoutes.TRANSACTIONS) {
        composable(LedgerRoutes.TRANSACTIONS) {
            TransactionsScreen(
                onAddTransaction = { navController.navigate(LedgerRoutes.addEdit()) },
                onOpenTransaction = { id -> navController.navigate(LedgerRoutes.addEdit(id)) },
                onOpenStatistics = { navController.navigate(LedgerRoutes.STATISTICS) },
                onOpenBudget = { navController.navigate(LedgerRoutes.BUDGET) },
                onOpenManage = { navController.navigate(LedgerRoutes.MANAGE) },
            )
        }
        composable(LedgerRoutes.MANAGE) {
            ManageScreen(onBack = { navController.popBackStack() })
        }
        composable(LedgerRoutes.BUDGET) {
            BudgetScreen(onBack = { navController.popBackStack() })
        }
        composable(LedgerRoutes.STATISTICS) {
            StatisticsScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = LedgerRoutes.ADD_EDIT,
            arguments = listOf(
                navArgument(LedgerRoutes.ARG_TRANSACTION_ID) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            ),
        ) {
            AddEditTransactionScreen(onDone = { navController.popBackStack() })
        }
    }
}
