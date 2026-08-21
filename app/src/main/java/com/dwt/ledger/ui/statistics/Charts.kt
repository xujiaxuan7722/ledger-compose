package com.dwt.ledger.ui.statistics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dwt.ledger.domain.logic.MonthPoint
import com.dwt.ledger.ui.theme.ExpenseRed
import com.dwt.ledger.ui.theme.IncomeGreen

/** 环形图的配色，按占比排名循环使用 */
val ChartPalette = listOf(
    Color(0xFF1E6F5C), Color(0xFFE07A5F), Color(0xFF3D5A80), Color(0xFFF2CC8F),
    Color(0xFF81B29A), Color(0xFF9C6ADE), Color(0xFF6D8EA0), Color(0xFFBC6C25),
)

fun sliceColor(index: Int): Color = ChartPalette[index % ChartPalette.size]

/**
 * 环形图：[fractions] 之和 ≤ 1，按顺序从 12 点方向顺时针画；中间放 [center] 内容。
 */
@Composable
fun DonutChart(
    fractions: List<Float>,
    modifier: Modifier = Modifier,
    strokeWidth: Float = 28f,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    center: @Composable () -> Unit = {},
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = Stroke(width = strokeWidth)
            val inset = strokeWidth / 2
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            val topLeft = Offset(inset, inset)
            drawArc(trackColor, 0f, 360f, false, topLeft, arcSize, style = stroke)
            var start = -90f
            fractions.forEachIndexed { i, f ->
                val sweep = (f * 360f).coerceAtLeast(0f)
                if (sweep > 0f) drawArc(sliceColor(i), start, sweep, false, topLeft, arcSize, style = stroke)
                start += sweep
            }
        }
        center()
    }
}

/**
 * 近几个月收入/支出分组柱状图。纵轴自适应最大值；横轴标月份（"3月"）。
 */
@Composable
fun TrendBarChart(points: List<MonthPoint>, modifier: Modifier = Modifier) {
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val density = LocalDensity.current
    val labelPx = with(density) { 11.sp.toPx() }
    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().height(180.dp)) {
            val maxCents = points.maxOfOrNull { maxOf(it.income.cents, it.expense.cents) }?.coerceAtLeast(1L) ?: 1L
            val bottomPad = labelPx * 2
            val chartH = size.height - bottomPad
            val groupW = size.width / points.size.coerceAtLeast(1)
            val barW = groupW * 0.28f
            val gap = groupW * 0.06f
            // 基线 + 两条参考线
            drawLine(gridColor, Offset(0f, chartH), Offset(size.width, chartH), 2f)
            listOf(0.5f, 1f).forEach { r -> drawLine(gridColor, Offset(0f, chartH * (1 - r)), Offset(size.width, chartH * (1 - r)), 1f) }
            val paint = android.graphics.Paint().apply {
                color = labelColor.toArgb(); textSize = labelPx
                textAlign = android.graphics.Paint.Align.CENTER; isAntiAlias = true
            }
            points.forEachIndexed { i, p ->
                val cx = groupW * i + groupW / 2
                val incH = chartH * p.income.cents / maxCents
                val expH = chartH * p.expense.cents / maxCents
                drawRect(IncomeGreen, Offset(cx - gap / 2 - barW, chartH - incH), Size(barW, incH))
                drawRect(ExpenseRed, Offset(cx + gap / 2, chartH - expH), Size(barW, expH))
                drawContext.canvas.nativeCanvas.drawText("${p.yearMonth.monthValue}月", cx, size.height - labelPx * 0.4f, paint)
            }
        }
    }
}

@Composable
fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(color, CircleShape))
        Text("  $label", style = MaterialTheme.typography.labelMedium)
    }
}
