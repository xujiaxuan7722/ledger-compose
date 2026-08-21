package com.dwt.ledger.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dwt.ledger.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** 底部导航与菜单入口能到达各页面 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AppNavigationTest {
    @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
    @get:Rule(order = 1) val composeRule = createAndroidComposeRule<MainActivity>()

    @Before fun setUp() = hiltRule.inject()

    @Test fun bottomBar_switchesBetweenTopLevelScreens() {
        composeRule.onNodeWithText("统计").performClick()
        composeRule.onNodeWithText("支出构成").assertIsDisplayed()
        composeRule.onNodeWithText("预算").performClick()
        composeRule.onNodeWithText("本月还没有预算，点右下角设置一个").assertIsDisplayed()
        composeRule.onNodeWithText("流水").performClick()
        composeRule.onNodeWithText("本月结余").assertIsDisplayed()
    }

    @Test fun overflowMenu_opensManageScreen_andBackReturns() {
        composeRule.onNodeWithContentDescription("更多").performClick()
        composeRule.onNodeWithText("分类与账户").performClick()
        composeRule.onNodeWithText("账户").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("返回").performClick()
        composeRule.onNodeWithText("本月结余").assertIsDisplayed()
    }

    @Test fun quickSearch_opensSearchScreen() {
        composeRule.onNodeWithText("搜索").performClick()
        composeRule.onNodeWithText("搜备注或分类名").assertIsDisplayed()
    }
}
