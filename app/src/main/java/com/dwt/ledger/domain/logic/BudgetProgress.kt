package com.dwt.ledger.domain.logic

import com.dwt.ledger.domain.model.Budget
import com.dwt.ledger.domain.model.Money
import com.dwt.ledger.domain.model.Transaction
import com.dwt.ledger.domain.model.TransactionKind

/** 一条预算的执行情况 */
data class BudgetProgress(
    val budget: Budget,
    val spent: Money,
) {
    val remaining: Money get() = budget.limit - spent
    val isOver: Boolean get() = spent > budget.limit
    /** 0.0 起，可超过 1.0（超支）；预算额为 0 时按 1.0/0.0 处理 */
    val fraction: Double
        get() = when {
            budget.limit.cents > 0 -> spent.cents.toDouble() / budget.limit.cents
            spent.cents > 0 -> 1.0
            else -> 0.0
        }
}

/**
 * 纯函数：给定某月的预算列表和该月流水，算出每条预算已花多少。
 * 只统计支出；总预算统计全部支出，分类预算只统计该分类。总预算排最前，其余按已花占比降序。
 */
fun computeBudgetProgress(budgets: List<Budget>, monthTransactions: List<Transaction>): List<BudgetProgress> {
    val expenses = monthTransactions.filter { it.kind == TransactionKind.EXPENSE }
    val totalSpent = expenses.sumOf { it.amount.cents }
    val spentByCategory = expenses.groupBy { it.categoryId }.mapValues { (_, l) -> l.sumOf { it.amount.cents } }
    return budgets.map { b ->
        val spent = if (b.categoryId == null) totalSpent else spentByCategory[b.categoryId] ?: 0L
        BudgetProgress(b, Money(spent))
    }.sortedWith(compareBy<BudgetProgress> { !it.budget.isTotal }.thenByDescending { it.fraction })
}
