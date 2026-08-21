package com.dwt.ledger.data

import com.dwt.ledger.data.local.AccountDao
import com.dwt.ledger.data.local.CategoryDao
import com.dwt.ledger.domain.model.Account
import com.dwt.ledger.domain.model.Category
import com.dwt.ledger.domain.model.TransactionKind
import javax.inject.Inject
import javax.inject.Singleton

/** 首次启动写入内置分类与账户；已有数据则什么都不做。 */
@Singleton
class DefaultDataSeeder @Inject constructor(
    private val categoryDao: CategoryDao,
    private val accountDao: AccountDao,
) {
    suspend fun seedIfEmpty() {
        if (categoryDao.count() == 0) {
            categoryDao.upsertAll(DEFAULT_CATEGORIES.map { it.toLocal() })
        }
        if (accountDao.count() == 0) {
            accountDao.upsertAll(DEFAULT_ACCOUNTS.map { it.toLocal() })
        }
    }

    companion object {
        // id 固定，便于测试和将来迁移
        val DEFAULT_CATEGORIES: List<Category> = listOf(
            Category("cat_food", "餐饮", TransactionKind.EXPENSE, "restaurant", 10, builtin = true),
            Category("cat_transport", "交通", TransactionKind.EXPENSE, "directions_bus", 20, builtin = true),
            Category("cat_shopping", "购物", TransactionKind.EXPENSE, "shopping_bag", 30, builtin = true),
            Category("cat_housing", "居住", TransactionKind.EXPENSE, "home", 40, builtin = true),
            Category("cat_fun", "娱乐", TransactionKind.EXPENSE, "sports_esports", 50, builtin = true),
            Category("cat_health", "医疗", TransactionKind.EXPENSE, "local_hospital", 60, builtin = true),
            Category("cat_study", "学习", TransactionKind.EXPENSE, "school", 70, builtin = true),
            Category("cat_other_expense", "其他支出", TransactionKind.EXPENSE, "more_horiz", 90, builtin = true),
            Category("cat_salary", "工资", TransactionKind.INCOME, "payments", 110, builtin = true),
            Category("cat_bonus", "奖金", TransactionKind.INCOME, "card_giftcard", 120, builtin = true),
            Category("cat_invest", "理财", TransactionKind.INCOME, "trending_up", 130, builtin = true),
            Category("cat_parttime", "兼职", TransactionKind.INCOME, "work", 140, builtin = true),
            Category("cat_other_income", "其他收入", TransactionKind.INCOME, "more_horiz", 190, builtin = true),
        )
        val DEFAULT_ACCOUNTS: List<Account> = listOf(
            Account("acc_cash", "现金", "wallet", 10),
            Account("acc_wechat", "微信", "chat", 20),
            Account("acc_alipay", "支付宝", "account_balance_wallet", 30),
            Account("acc_bank", "银行卡", "credit_card", 40),
        )
    }
}
