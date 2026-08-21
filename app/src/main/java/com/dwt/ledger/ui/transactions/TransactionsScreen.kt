package com.dwt.ledger.ui.transactions

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material.icons.outlined.Search
import com.dwt.ledger.ui.theme.BrandBlue
import com.dwt.ledger.ui.theme.BrandBlueDark
import com.dwt.ledger.ui.theme.BrandNavy
import com.dwt.ledger.ui.theme.HeroGradient
import com.dwt.ledger.ui.theme.TileBlue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.dwt.ledger.ui.datatransfer.CsvEvent
import com.dwt.ledger.ui.datatransfer.CsvViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dwt.ledger.R
import com.dwt.ledger.domain.logic.MonthlySummary
import com.dwt.ledger.domain.model.Money
import com.dwt.ledger.domain.model.TransactionKind
import com.dwt.ledger.ui.common.MonthSwitcher
import com.dwt.ledger.ui.common.displayDay
import com.dwt.ledger.ui.common.displaySigned
import com.dwt.ledger.ui.common.displayYuan
import com.dwt.ledger.ui.common.iconFor
import com.dwt.ledger.ui.theme.ExpenseRed
import com.dwt.ledger.ui.theme.IncomeGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    onAddTransaction: (TransactionKind) -> Unit,
    onOpenTransaction: (String) -> Unit,
    onOpenManage: () -> Unit,
    onOpenSearch: () -> Unit,
    viewModel: TransactionsViewModel = hiltViewModel(),
    csvViewModel: CsvViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val csvEvent by csvViewModel.event.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var menuOpen by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri: Uri? ->
        if (uri != null) csvViewModel.export { csv ->
            withContext(Dispatchers.IO) { context.contentResolver.openOutputStream(uri)!!.use { it.write(csv.toByteArray()) } }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) csvViewModel.import {
            withContext(Dispatchers.IO) { context.contentResolver.openInputStream(uri)!!.use { it.readBytes().toString(Charsets.UTF_8) } }
        }
    }
    csvEvent?.let { ev ->
        val text = when (ev) {
            is CsvEvent.Exported -> stringResource(R.string.export_done, ev.rows)
            is CsvEvent.Imported -> if (ev.summary.errors.isEmpty()) stringResource(R.string.import_done, ev.summary.imported)
                else stringResource(R.string.import_done_with_errors, ev.summary.imported, ev.summary.errors.size, ev.summary.errors.first())
            is CsvEvent.Failed -> stringResource(R.string.transfer_failed, ev.reason)
        }
        LaunchedEffect(ev) { snackbar.showSnackbar(text); csvViewModel.consumeEvent() }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0), // 顶层页面：状态栏/导航栏由 TopAppBar 与底部导航处理
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                title = { MonthSwitcher(uiState.yearMonth, viewModel::previousMonth, viewModel::nextMonth) },
                actions = {
                    IconButton(onClick = { menuOpen = true }) { Icon(Icons.Default.MoreVert, stringResource(R.string.more)) }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.manage_title)) }, leadingIcon = { Icon(Icons.Outlined.Tune, null) }, onClick = { menuOpen = false; onOpenManage() })
                        DropdownMenuItem(text = { Text(stringResource(R.string.export_csv)) }, onClick = {
                            menuOpen = false; exportLauncher.launch("ledger-${uiState.yearMonth}.csv")
                        })
                        DropdownMenuItem(text = { Text(stringResource(R.string.import_csv)) }, onClick = {
                            menuOpen = false; importLauncher.launch(arrayOf("text/*", "text/csv", "text/comma-separated-values", "application/octet-stream"))
                        })
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 88.dp),
        ) {
            item { HeroCard(uiState.summary, Modifier.padding(16.dp, 8.dp, 16.dp, 12.dp)) }
            item {
                QuickActions(
                    onExpense = { onAddTransaction(TransactionKind.EXPENSE) },
                    onIncome = { onAddTransaction(TransactionKind.INCOME) },
                    onSearch = onOpenSearch,
                    modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 12.dp),
                )
            }
            if (uiState.overBudgets.isNotEmpty()) {
                item { OverBudgetBanner(uiState.overBudgets, modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp)) }
            }
            if (uiState.isEmpty) {
                item {
                    Text(
                        stringResource(R.string.empty_month),
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            uiState.days.forEach { day ->
                item(key = "day-${day.date}") { DayHeader(day) }
                items(day.items, key = { it.id }) { item ->
                    TransactionRow(item, onClick = { onOpenTransaction(item.id) })
                }
            }
        }
    }
}

@Composable
private fun HeroCard(summary: MonthlySummary, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth()
            .shadow(10.dp, MaterialTheme.shapes.large, spotColor = BrandNavy.copy(alpha = 0.35f))
            .clip(MaterialTheme.shapes.large)
            .background(HeroGradient)
            .padding(22.dp),
    ) {
        Column {
            Text(stringResource(R.string.month_balance), color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            Text(summary.balance.displayYuan(), color = Color.White, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth()) {
                HeroStat(stringResource(R.string.income), summary.income, Modifier.weight(1f))
                HeroStat(stringResource(R.string.expense), summary.expense, Modifier.weight(1f))
            }
        }
        Icon(
            Icons.Outlined.AccountBalanceWallet, null,
            modifier = Modifier.align(Alignment.TopEnd).size(34.dp),
            tint = Color.White.copy(alpha = 0.9f),
        )
    }
}

@Composable
private fun HeroStat(label: String, amount: Money, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelMedium)
        Text(amount.displayYuan(), color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

/** 原项目首页的三个浅蓝圆角快捷按钮 */
@Composable
private fun QuickActions(onExpense: () -> Unit, onIncome: () -> Unit, onSearch: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        QuickAction(stringResource(R.string.quick_expense), Icons.Outlined.RemoveCircleOutline, onExpense, Modifier.weight(1f))
        QuickAction(stringResource(R.string.quick_income), Icons.Outlined.AddCircleOutline, onIncome, Modifier.weight(1f))
        QuickAction(stringResource(R.string.search), Icons.Outlined.Search, onSearch, Modifier.weight(1f))
    }
}

@Composable
private fun QuickAction(label: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(76.dp),
        shape = MaterialTheme.shapes.medium,
        color = TileBlue,
        border = BorderStroke(1.dp, BrandBlue.copy(alpha = 0.35f)),
    ) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = BrandBlue, modifier = Modifier.size(26.dp))
            Spacer(Modifier.height(6.dp))
            Text(label, color = BrandBlueDark, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun OverBudgetBanner(items: List<OverBudget>, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.onErrorContainer)
            Spacer(Modifier.width(12.dp))
            Text(
                stringResource(R.string.over_budget_banner, items.joinToString("、") { "${it.title} ${it.overBy.displayYuan()}" }),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}

@Composable
private fun DayHeader(day: DayGroup) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 14.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(day.date.displayDay(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text((if (day.net.cents >= 0) "+" else "") + day.net.displayYuan(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TransactionRow(item: TransactionItem, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = MaterialTheme.shapes.small, color = TileBlue, modifier = Modifier.size(44.dp)) {
                Icon(iconFor(item.categoryIcon), null, modifier = Modifier.padding(10.dp), tint = BrandBlue)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.categoryName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                val sub = listOf(item.accountName, item.note).filter { it.isNotBlank() }.joinToString(" · ")
                if (sub.isNotBlank()) Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                item.amount.displaySigned(item.kind),
                style = MaterialTheme.typography.titleSmall,
                color = if (item.kind == TransactionKind.INCOME) IncomeGreen else ExpenseRed,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
