package com.dwt.ledger.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dwt.ledger.R
import com.dwt.ledger.domain.model.TransactionKind
import com.dwt.ledger.ui.common.displayDate
import com.dwt.ledger.ui.common.displaySigned
import com.dwt.ledger.ui.common.displayYuan
import com.dwt.ledger.ui.common.iconFor
import com.dwt.ledger.ui.theme.ExpenseRed
import com.dwt.ledger.ui.theme.IncomeGreen

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(onBack: () -> Unit, onOpenTransaction: (String) -> Unit, viewModel: SearchViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.search)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } },
                actions = { if (state.hasActiveFilter) TextButton(onClick = viewModel::clearAll) { Text(stringResource(R.string.clear_filters)) } },
            )
        },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = state.query, onValueChange = viewModel::setQuery, singleLine = true,
                        placeholder = { Text(stringResource(R.string.search_hint)) },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        trailingIcon = { if (state.query.isNotEmpty()) IconButton(onClick = { viewModel.setQuery("") }) { Icon(Icons.Default.Clear, null) } },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    FilterLabel(stringResource(R.string.date))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DateRangePreset.entries.forEach { r ->
                            FilterChip(selected = state.range == r, onClick = { viewModel.setRange(r) }, label = { Text(stringResource(r.label())) })
                        }
                    }
                    FilterLabel(stringResource(R.string.kind))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TransactionKind.entries.forEach { k ->
                            FilterChip(selected = state.kind == k, onClick = { viewModel.toggleKind(k) }, label = { Text(stringResource(if (k == TransactionKind.INCOME) R.string.income else R.string.expense)) })
                        }
                    }
                    FilterLabel(stringResource(R.string.category))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.visibleCategories.forEach { c ->
                            FilterChip(selected = c.id in state.categoryIds, onClick = { viewModel.toggleCategory(c.id) }, label = { Text(c.name) }, leadingIcon = { Icon(iconFor(c.icon), null, Modifier.size(18.dp)) })
                        }
                    }
                    FilterLabel(stringResource(R.string.account))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.accounts.forEach { a ->
                            FilterChip(selected = a.id in state.accountIds, onClick = { viewModel.toggleAccount(a.id) }, label = { Text(a.name) })
                        }
                    }
                    HorizontalDivider(Modifier.padding(top = 6.dp))
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.result_count, state.results.size), style = MaterialTheme.typography.labelLarge)
                        Text(
                            stringResource(R.string.result_summary, state.summary.income.displayYuan(), state.summary.expense.displayYuan()),
                            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (!state.isLoading && state.results.isEmpty()) {
                item { Text(stringResource(R.string.no_results), Modifier.fillMaxWidth().padding(32.dp), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            items(state.results, key = { it.item.id }) { row ->
                Row(
                    Modifier.fillMaxWidth().clickable { onOpenTransaction(row.item.id) }.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(40.dp)) {
                        Icon(iconFor(row.item.categoryIcon), null, Modifier.padding(8.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(row.item.categoryName, style = MaterialTheme.typography.bodyLarge)
                        val sub = listOf(row.date.displayDate(), row.item.accountName, row.item.note).filter { it.isNotBlank() }.joinToString(" · ")
                        Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        row.item.amount.displaySigned(row.item.kind), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium,
                        color = if (row.item.kind == TransactionKind.INCOME) IncomeGreen else ExpenseRed,
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterLabel(text: String) = Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)

private fun DateRangePreset.label(): Int = when (this) {
    DateRangePreset.ALL -> R.string.range_all
    DateRangePreset.THIS_MONTH -> R.string.range_this_month
    DateRangePreset.LAST_3_MONTHS -> R.string.range_last_3_months
    DateRangePreset.THIS_YEAR -> R.string.range_this_year
}
