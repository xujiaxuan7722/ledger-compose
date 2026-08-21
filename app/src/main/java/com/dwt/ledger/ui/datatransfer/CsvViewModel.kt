package com.dwt.ledger.ui.datatransfer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dwt.ledger.data.CsvTransfer
import com.dwt.ledger.data.ImportSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface CsvEvent {
    data class Exported(val rows: Int) : CsvEvent
    data class Imported(val summary: ImportSummary) : CsvEvent
    data class Failed(val reason: String) : CsvEvent
}

/** 负责 CSV 内容的生成与导入；文件读写（SAF）由界面完成，ViewModel 只处理字符串 */
@HiltViewModel
class CsvViewModel @Inject constructor(
    private val transfer: CsvTransfer,
) : ViewModel() {
    private val _event = MutableStateFlow<CsvEvent?>(null)
    val event: StateFlow<CsvEvent?> = _event
    fun consumeEvent() = _event.update { null }

    /** 生成 CSV 文本并交给 [write]（在 IO 线程写文件）；成功后发事件 */
    fun export(write: suspend (String) -> Unit) {
        viewModelScope.launch {
            try {
                val csv = transfer.export()
                write(csv)
                _event.update { CsvEvent.Exported(csv.lineSequence().count { it.isNotBlank() } - 1) }
            } catch (e: Exception) {
                _event.update { CsvEvent.Failed(e.message ?: "导出失败") }
            }
        }
    }

    fun import(read: suspend () -> String) {
        viewModelScope.launch {
            try {
                _event.update { CsvEvent.Imported(transfer.import(read())) }
            } catch (e: Exception) {
                _event.update { CsvEvent.Failed(e.message ?: "导入失败") }
            }
        }
    }
}
