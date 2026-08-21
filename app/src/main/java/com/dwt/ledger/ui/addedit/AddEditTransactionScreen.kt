package com.dwt.ledger.ui.addedit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dwt.ledger.R
import com.dwt.ledger.domain.model.TransactionKind
import com.dwt.ledger.ui.common.displayDate
import com.dwt.ledger.ui.common.iconFor
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditTransactionScreen(
    onDone: () -> Unit,
    viewModel: AddEditTransactionViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(state.isSaved, state.isDeleted) { if (state.isSaved || state.isDeleted) onDone() }
    state.errorMessage?.let { res ->
        val message = stringResource(res)
        LaunchedEffect(res) {
            snackbarHostState.showSnackbar(message)
            viewModel.consumeError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (state.isEditing) R.string.edit_transaction else R.string.add_transaction)) },
                navigationIcon = {
                    IconButton(onClick = onDone) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) }
                },
                actions = {
                    if (state.isEditing) {
                        IconButton(onClick = viewModel::delete) { Icon(Icons.Default.Delete, stringResource(R.string.delete)) }
                    }
                    IconButton(onClick = viewModel::save) { Icon(Icons.Default.Check, stringResource(R.string.save)) }
                },
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
                    ) {
                        Text(stringResource(if (kind == TransactionKind.INCOME) R.string.income else R.string.expense))
                    }
                }
            }

            OutlinedTextField(
                value = state.amountText,
                onValueChange = viewModel::setAmount,
                label = { Text(stringResource(R.string.amount)) },
                placeholder = { Text(stringResource(R.string.amount_hint)) },
                prefix = { Text("¥") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.fillMaxWidth(),
            )

            Section(stringResource(R.string.category)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.visibleCategories.forEach { c ->
                        FilterChip(
                            selected = state.categoryId == c.id,
                            onClick = { viewModel.selectCategory(c.id) },
                            label = { Text(c.name) },
                            leadingIcon = { Icon(iconFor(c.icon), null) },
                        )
                    }
                }
            }

            Section(stringResource(R.string.account)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.accounts.forEach { a ->
                        FilterChip(
                            selected = state.accountId == a.id,
                            onClick = { viewModel.selectAccount(a.id) },
                            label = { Text(a.name) },
                            leadingIcon = { Icon(iconFor(a.icon), null) },
                        )
                    }
                }
            }

            Section(stringResource(R.string.date)) {
                OutlinedButton(onClick = { showDatePicker = true }) {
                    Icon(Icons.Default.DateRange, null)
                    Spacer(Modifier.height(0.dp).padding(horizontal = 4.dp))
                    Text("  " + state.date.displayDate())
                }
            }

            OutlinedTextField(
                value = state.note,
                onValueChange = viewModel::setNote,
                label = { Text(stringResource(R.string.note)) },
                placeholder = { Text(stringResource(R.string.note_hint)) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        viewModel.setDate(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    showDatePicker = false
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel)) } },
        ) { DatePicker(state = pickerState) }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        content()
    }
}
