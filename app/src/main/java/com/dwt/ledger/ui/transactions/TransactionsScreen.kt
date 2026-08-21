package com.dwt.ledger.ui.transactions

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
    onAddTransaction: () -> Unit,
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
                title = { MonthSwitcher(uiState.yearMonth, viewModel::previousMonth, viewModel::nextMonth) },
                actions = {
                    IconButton(onClick = onOpenSearch) {
                        Icon(Icons.Default.Search, stringResource(R.string.search))
                    }
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
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTransaction) {
                Icon(Icons.Default.Add, stringResource(R.string.add_transaction))
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 88.dp),
        ) {
            item { SummaryCard(uiState.summary, Modifier.padding(16.dp)) }
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
private fun SummaryCard(summary: MonthlySummary, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            SummaryItem(stringResource(R.string.income), summary.income, IncomeGreen)
            SummaryItem(stringResource(R.string.expense), summary.expense, ExpenseRed)
            SummaryItem(stringResource(R.string.balance), summary.balance, MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
private fun OverBudgetBanner(items: List<OverBudget>, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
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
private fun SummaryItem(label: String, amount: Money, color: Color) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
        Spacer(Modifier.height(4.dp))
        Text(amount.displayYuan(), style = MaterialTheme.typography.titleMedium, color = color, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DayHeader(day: DayGroup) {
    Column {
        HorizontalDivider()
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(day.date.displayDay(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text((if (day.net.cents >= 0) "+" else "") + day.net.displayYuan(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TransactionRow(item: TransactionItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(40.dp)) {
            Icon(iconFor(item.categoryIcon), null, modifier = Modifier.padding(8.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(item.categoryName, style = MaterialTheme.typography.bodyLarge)
            val sub = listOf(item.accountName, item.note).filter { it.isNotBlank() }.joinToString(" · ")
            if (sub.isNotBlank()) Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            item.amount.displaySigned(item.kind),
            style = MaterialTheme.typography.bodyLarge,
            color = if (item.kind == TransactionKind.INCOME) IncomeGreen else ExpenseRed,
            fontWeight = FontWeight.Medium,
        )
    }
}
