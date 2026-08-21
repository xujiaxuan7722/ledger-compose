package com.dwt.ledger.ui.budget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dwt.ledger.R
import com.dwt.ledger.ui.common.MonthSwitcher
import com.dwt.ledger.ui.common.displayYuan
import com.dwt.ledger.ui.common.iconFor
import com.dwt.ledger.ui.theme.ExpenseRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(onBack: () -> Unit, viewModel: BudgetViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { MonthSwitcher(state.yearMonth, viewModel::previousMonth, viewModel::nextMonth) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.openEditor() }) { Icon(Icons.Default.Add, stringResource(R.string.add_budget)) }
        },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 88.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (!state.isLoading && state.items.isEmpty()) {
                item {
                    Text(stringResource(R.string.empty_budget), Modifier.fillMaxWidth().padding(32.dp), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            items(state.items, key = { it.progress.budget.id }) { item -> BudgetCard(item, onClick = { viewModel.openEditor(item) }) }
        }
    }

    state.editor?.let { ed -> BudgetEditorDialog(ed, state, viewModel) }
}

@Composable
private fun BudgetCard(item: BudgetItem, onClick: () -> Unit) {
    val p = item.progress
    val barColor = if (p.isOver) ExpenseRed else MaterialTheme.colorScheme.primary
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(iconFor(item.icon), null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(8.dp))
                Text(item.title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                Text(
                    if (p.isOver) stringResource(R.string.over_by, (p.spent - p.budget.limit).displayYuan())
                    else stringResource(R.string.remaining, p.remaining.displayYuan()),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (p.isOver) ExpenseRed else MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                )
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { p.fraction.toFloat().coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = barColor, trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.spent_of, p.spent.displayYuan(), p.budget.limit.displayYuan()),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BudgetEditorDialog(ed: BudgetEditor, state: BudgetUiState, viewModel: BudgetViewModel) {
    AlertDialog(
        onDismissRequest = viewModel::closeEditor,
        title = { Text(stringResource(if (ed.budgetId == null) R.string.add_budget else R.string.edit_budget)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.budget_scope), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = ed.categoryId == null, onClick = { viewModel.editorSelectCategory(null) }, label = { Text(stringResource(R.string.total_budget)) })
                    state.expenseCategories.forEach { c ->
                        FilterChip(selected = ed.categoryId == c.id, onClick = { viewModel.editorSelectCategory(c.id) }, label = { Text(c.name) }, leadingIcon = { Icon(iconFor(c.icon), null) })
                    }
                }
                OutlinedTextField(
                    value = ed.amountText, onValueChange = viewModel::editorSetAmount,
                    label = { Text(stringResource(R.string.budget_limit)) }, prefix = { Text("¥") },
                    isError = ed.error, supportingText = if (ed.error) ({ Text(stringResource(R.string.error_amount_invalid)) }) else null,
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = { TextButton(onClick = viewModel::saveEditor) { Text(stringResource(R.string.save)) } },
        dismissButton = {
            Row {
                if (ed.budgetId != null) TextButton(onClick = viewModel::deleteEditing) { Text(stringResource(R.string.delete), color = ExpenseRed) }
                TextButton(onClick = viewModel::closeEditor) { Text(stringResource(R.string.cancel)) }
            }
        },
    )
}
