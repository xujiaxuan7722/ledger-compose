package com.dwt.ledger.ui.statistics

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dwt.ledger.R
import com.dwt.ledger.domain.model.TransactionKind
import com.dwt.ledger.ui.common.MonthSwitcher
import com.dwt.ledger.ui.common.displayYuan
import com.dwt.ledger.ui.common.iconFor
import com.dwt.ledger.ui.theme.ExpenseRed
import com.dwt.ledger.ui.theme.IncomeGreen
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(viewModel: StatisticsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        contentWindowInsets = WindowInsets(0), // 顶层页面：状态栏/导航栏由 TopAppBar 与底部导航处理
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                title = { MonthSwitcher(state.yearMonth, viewModel::previousMonth, viewModel::nextMonth) },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                TransactionKind.entries.forEachIndexed { index, kind ->
                    SegmentedButton(
                        selected = state.kind == kind,
                        onClick = { viewModel.setKind(kind) },
                        shape = SegmentedButtonDefaults.itemShape(index, TransactionKind.entries.size),
                    ) { Text(stringResource(if (kind == TransactionKind.INCOME) R.string.income_share else R.string.expense_share)) }
                }
            }

            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    DonutChart(
                        fractions = state.shares.map { it.fraction.toFloat() },
                        modifier = Modifier.size(180.dp),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(stringResource(R.string.total), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(state.total.displayYuan(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    if (state.shares.isEmpty()) {
                        Text(stringResource(R.string.empty_statistics), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        state.shares.forEachIndexed { index, item -> ShareRow(index, item) }
                    }
                }
            }

            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.trend_title, TREND_MONTHS), style = MaterialTheme.typography.titleSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            LegendDot(IncomeGreen, stringResource(R.string.income))
                            LegendDot(ExpenseRed, stringResource(R.string.expense))
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    TrendBarChart(state.trend, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun ShareRow(index: Int, item: CategoryShareItem) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.size(10.dp).background(sliceColor(index), CircleShape))
            Spacer(Modifier.width(8.dp))
            Icon(iconFor(item.icon), null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(6.dp))
            Text(item.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Text("${(item.fraction * 100).roundToInt()}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(12.dp))
            Text(item.amount.displayYuan(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { item.fraction.toFloat() },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = sliceColor(index),
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}
