package com.dwt.ledger.ui.transactions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.PieChart
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
    onOpenStatistics: () -> Unit,
    viewModel: TransactionsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { MonthSwitcher(uiState.yearMonth, viewModel::previousMonth, viewModel::nextMonth) },
                actions = {
                    IconButton(onClick = onOpenStatistics) {
                        Icon(Icons.Default.PieChart, stringResource(R.string.statistics))
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
