package com.dwt.ledger.ui.manage

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dwt.ledger.R
import com.dwt.ledger.domain.model.TransactionKind
import com.dwt.ledger.ui.common.AvailableIcons
import com.dwt.ledger.ui.common.iconFor
import com.dwt.ledger.ui.theme.ExpenseRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageScreen(onBack: () -> Unit, viewModel: ManageViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    state.message?.let { msg ->
        val text = when (msg) { is UiMessage.InUse -> stringResource(R.string.error_in_use, msg.count) }
        LaunchedEffect(msg) { snackbar.showSnackbar(text); viewModel.consumeMessage() }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.manage_title)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::openCreate, containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) { Icon(Icons.Default.Add, stringResource(R.string.add)) }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = state.tab.ordinal) {
                Tab(selected = state.tab == ManageTab.CATEGORIES, onClick = { viewModel.selectTab(ManageTab.CATEGORIES) }, text = { Text(stringResource(R.string.category)) })
                Tab(selected = state.tab == ManageTab.ACCOUNTS, onClick = { viewModel.selectTab(ManageTab.ACCOUNTS) }, text = { Text(stringResource(R.string.account)) })
            }
            LazyColumn(contentPadding = PaddingValues(bottom = 88.dp)) {
                when (state.tab) {
                    ManageTab.CATEGORIES -> {
                        item { SectionHeader(stringResource(R.string.expense)) }
                        items(state.expenseCategories, key = { "c-" + it.id }) { c -> ItemRow(c.icon, c.name, null) { viewModel.openEdit(c) } }
                        item { SectionHeader(stringResource(R.string.income)) }
                        items(state.incomeCategories, key = { "c-" + it.id }) { c -> ItemRow(c.icon, c.name, null) { viewModel.openEdit(c) } }
                    }
                    ManageTab.ACCOUNTS -> items(state.accounts, key = { "a-" + it.id }) { a ->
                        ItemRow(a.icon, a.name, if (a.archived) stringResource(R.string.archived) else null) { viewModel.openEdit(a) }
                    }
                }
            }
        }
    }

    state.editor?.let { ed -> EditorDialog(ed, viewModel) }
}

@Composable
private fun SectionHeader(text: String) {
    Text(text, Modifier.padding(16.dp, 16.dp, 16.dp, 4.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun ItemRow(icon: String, name: String, badge: String?, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(36.dp)) {
            Icon(iconFor(icon), null, Modifier.padding(7.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
        }
        Spacer(Modifier.width(12.dp))
        Text(name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        if (badge != null) Text(badge, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditorDialog(ed: ItemEditor, viewModel: ManageViewModel) {
    val isCategory = ed.tab == ManageTab.CATEGORIES
    AlertDialog(
        onDismissRequest = viewModel::closeEditor,
        title = {
            Text(stringResource(when {
                ed.id == null && isCategory -> R.string.add_category
                ed.id == null -> R.string.add_account
                isCategory -> R.string.edit_category
                else -> R.string.edit_account
            }))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (isCategory && ed.id == null) {
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        TransactionKind.entries.forEachIndexed { i, k ->
                            SegmentedButton(selected = ed.kind == k, onClick = { viewModel.editorSetKind(k) }, shape = SegmentedButtonDefaults.itemShape(i, 2)) {
                                Text(stringResource(if (k == TransactionKind.INCOME) R.string.income else R.string.expense))
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = ed.name, onValueChange = viewModel::editorSetName, singleLine = true,
                    label = { Text(stringResource(R.string.name)) }, isError = ed.nameError,
                    supportingText = if (ed.nameError) ({ Text(stringResource(R.string.error_name_required)) }) else null,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(stringResource(R.string.icon), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AvailableIcons.forEach { key ->
                        FilterChip(selected = ed.icon == key, onClick = { viewModel.editorSetIcon(key) }, label = { Icon(iconFor(key), key, Modifier.size(20.dp)) })
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = viewModel::saveEditor) { Text(stringResource(R.string.save)) } },
        dismissButton = {
            Row {
                if (ed.id != null && !isCategory) {
                    TextButton(onClick = viewModel::toggleArchiveEditing) { Text(stringResource(if (ed.archived) R.string.unarchive else R.string.archive)) }
                }
                if (ed.id != null) TextButton(onClick = viewModel::deleteEditing) { Text(stringResource(R.string.delete), color = ExpenseRed) }
                TextButton(onClick = viewModel::closeEditor) { Text(stringResource(R.string.cancel)) }
            }
        },
    )
}
