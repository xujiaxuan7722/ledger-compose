package com.dwt.ledger.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dwt.ledger.MainActivity
import com.dwt.ledger.data.DefaultDataSeeder
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/** 端到端：记一笔 → 回到流水页看到这条记录与汇总 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AddTransactionFlowTest {
    @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
    @get:Rule(order = 1) val composeRule = createAndroidComposeRule<MainActivity>()

    @Inject lateinit var seeder: DefaultDataSeeder

    @Before fun setUp() {
        hiltRule.inject()
        runBlocking { seeder.seedIfEmpty() }
    }

    @Test fun addExpense_showsInListAndSummary() {
        composeRule.onNodeWithText("这个月还没有记录，点上方「记支出」开始").assertIsDisplayed()
        composeRule.onNodeWithText("记支出").performClick()
        composeRule.onNodeWithText("记一笔").assertIsDisplayed()

        composeRule.onNodeWithText("金额").performTextInput("12.5")
        composeRule.onNodeWithText("餐饮").performClick()
        composeRule.onNodeWithText("现金").performClick()
        composeRule.onNodeWithText("备注").performTextInput("UI 测试")
        composeRule.onNodeWithContentDescription("保存").performClick()

        // 保存是异步写库 + 返回动画，等流水页真正显示出来
        composeRule.waitUntilDisplayed("本月结余")
        composeRule.onNodeWithText("餐饮").assertIsDisplayed()
        composeRule.onNodeWithText("现金 · UI 测试").assertIsDisplayed()
        // 金额会出现在结余卡、当日小计和条目里，只断言至少一处可见
        composeRule.onAllNodesWithText("-¥12.50").onFirst().assertIsDisplayed()
    }

    @Test fun save_withoutAmount_showsValidationMessage() {
        composeRule.onNodeWithText("记收入").performClick()
        composeRule.onNodeWithContentDescription("保存").performClick()
        composeRule.waitUntilDisplayed("请输入大于 0 的金额")
    }

    @Test fun incomeEntry_startsWithIncomeSelectedAndIncomeCategories() {
        composeRule.onNodeWithText("记收入").performClick()
        composeRule.onNodeWithText("工资").assertIsDisplayed()   // 收入分类可见
        composeRule.onNodeWithText("餐饮").assertDoesNotExist()  // 支出分类不可见
    }
}

/** 轮询直到某段文字真正可见（默认 5 秒），避免异步写库/导航动画造成的偶发失败 */
fun ComposeTestRule.waitUntilDisplayed(text: String, timeoutMillis: Long = 5_000) {
    waitUntil(timeoutMillis) {
        onAllNodesWithText(text).fetchSemanticsNodes().any { true } &&
            runCatching { onAllNodesWithText(text).onFirst().isDisplayed() }.getOrDefault(false)
    }
}
