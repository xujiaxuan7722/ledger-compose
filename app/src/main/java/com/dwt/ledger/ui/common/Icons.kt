package com.dwt.ledger.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Wallet
import androidx.compose.material.icons.outlined.Work
import androidx.compose.ui.graphics.vector.ImageVector

/** 分类/账户的图标键 → Material 图标；未知键回退到通用图标 */
fun iconFor(key: String): ImageVector = when (key) {
    "restaurant" -> Icons.Outlined.Restaurant
    "directions_bus" -> Icons.Outlined.DirectionsBus
    "shopping_bag" -> Icons.Outlined.ShoppingBag
    "home" -> Icons.Outlined.Home
    "sports_esports" -> Icons.Outlined.SportsEsports
    "local_hospital" -> Icons.Outlined.LocalHospital
    "school" -> Icons.Outlined.School
    "more_horiz" -> Icons.Outlined.MoreHoriz
    "payments" -> Icons.Outlined.Payments
    "card_giftcard" -> Icons.Outlined.CardGiftcard
    "trending_up" -> Icons.AutoMirrored.Outlined.TrendingUp
    "work" -> Icons.Outlined.Work
    "wallet" -> Icons.Outlined.Wallet
    "chat" -> Icons.AutoMirrored.Outlined.Chat
    "account_balance_wallet" -> Icons.Outlined.AccountBalanceWallet
    "credit_card" -> Icons.Outlined.CreditCard
    else -> Icons.Outlined.Category
}
