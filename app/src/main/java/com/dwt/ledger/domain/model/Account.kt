package com.dwt.ledger.domain.model

data class Account(
    val id: String,
    val name: String,
    val icon: String,
    val sortOrder: Int,
    val archived: Boolean = false,
)
