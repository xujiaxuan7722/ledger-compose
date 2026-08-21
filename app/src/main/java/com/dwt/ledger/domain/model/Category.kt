package com.dwt.ledger.domain.model

data class Category(
    val id: String,
    val name: String,
    val kind: TransactionKind,
    /** 图标键，UI 层映射到具体图标；未知键回退默认图标 */
    val icon: String,
    val sortOrder: Int,
    val builtin: Boolean = false,
)
