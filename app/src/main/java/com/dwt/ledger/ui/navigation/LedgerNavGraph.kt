package com.dwt.ledger.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dwt.ledger.R
import com.dwt.ledger.ui.addedit.AddEditTransactionScreen
import com.dwt.ledger.ui.budget.BudgetScreen
import com.dwt.ledger.ui.manage.ManageScreen
import com.dwt.ledger.ui.search.SearchScreen
import com.dwt.ledger.ui.statistics.StatisticsScreen
import com.dwt.ledger.ui.transactions.TransactionsScreen

object LedgerRoutes {
    const val TRANSACTIONS = "transactions"
    const val STATISTICS = "statistics"
    const val BUDGET = "budget"
    const val MANAGE = "manage"
    const val SEARCH = "search"
    const val ARG_TRANSACTION_ID = "transactionId"
    const val ADD_EDIT = "addEdit?$ARG_TRANSACTION_ID={$ARG_TRANSACTION_ID}"
    fun addEdit(transactionId: String? = null): String =
        if (transactionId == null) "addEdit" else "addEdit?$ARG_TRANSACTION_ID=$transactionId"
}

/** 底部导航的三个顶层目的地 */
private data class TopLevel(val route: String, val labelRes: Int, val selected: ImageVector, val unselected: ImageVector)

private val TOP_LEVEL = listOf(
    TopLevel(LedgerRoutes.TRANSACTIONS, R.string.transactions_title, Icons.AutoMirrored.Filled.ReceiptLong, Icons.AutoMirrored.Outlined.ReceiptLong),
    TopLevel(LedgerRoutes.STATISTICS, R.string.statistics, Icons.Filled.PieChart, Icons.Outlined.PieChart),
    TopLevel(LedgerRoutes.BUDGET, R.string.budget, Icons.Filled.Savings, Icons.Outlined.Savings),
)

@Composable
fun LedgerNavGraph(navController: NavHostController = rememberNavController()) {
    val backStack by navController.currentBackStackEntryAsState()
    val currentDestination = backStack?.destination
    val showBottomBar = TOP_LEVEL.any { t -> currentDestination?.hierarchy?.any { it.route == t.route } == true }

    Scaffold(
        // 状态栏由各页 TopAppBar 处理、导航栏由底栏处理，这里不再叠加一次
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    TOP_LEVEL.forEach { t ->
                        val selected = currentDestination?.hierarchy?.any { it.route == t.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(t.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(if (selected) t.selected else t.unselected, null) },
                            label = { Text(stringResource(t.labelRes)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(navController = navController, startDestination = LedgerRoutes.TRANSACTIONS, modifier = Modifier.padding(padding)) {
            composable(LedgerRoutes.TRANSACTIONS) {
                TransactionsScreen(
                    onAddTransaction = { navController.navigate(LedgerRoutes.addEdit()) },
                    onOpenTransaction = { id -> navController.navigate(LedgerRoutes.addEdit(id)) },
                    onOpenManage = { navController.navigate(LedgerRoutes.MANAGE) },
                    onOpenSearch = { navController.navigate(LedgerRoutes.SEARCH) },
                )
            }
            composable(LedgerRoutes.STATISTICS) { StatisticsScreen() }
            composable(LedgerRoutes.BUDGET) { BudgetScreen() }
            composable(LedgerRoutes.MANAGE) { ManageScreen(onBack = { navController.popBackStack() }) }
            composable(LedgerRoutes.SEARCH) {
                SearchScreen(
                    onBack = { navController.popBackStack() },
                    onOpenTransaction = { id -> navController.navigate(LedgerRoutes.addEdit(id)) },
                )
            }
            composable(
                route = LedgerRoutes.ADD_EDIT,
                arguments = listOf(navArgument(LedgerRoutes.ARG_TRANSACTION_ID) { type = NavType.StringType; nullable = true; defaultValue = null }),
            ) {
                AddEditTransactionScreen(onDone = { navController.popBackStack() })
            }
        }
    }
}
