package com.example.finplanner

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

fun formatRub(value: Number): String {
    val nf = NumberFormat.getNumberInstance(Locale("ru", "RU")).apply {
        maximumFractionDigits = 0
    }
    return "${nf.format(value)} ₽"
}

@Composable
fun InteractiveGrowthChart(
    points: List<MonthPoint>,
    modifier: Modifier = Modifier
) {
    if (points.size < 2) return

    val view = LocalView.current
    var selectedIndex by remember(points) { mutableStateOf<Int?>(null) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = Color(0xFF10B981)
    val surfaceVariant = MaterialTheme.colorScheme.surfaceContainerHigh

    val activePoint = selectedIndex?.let { points.getOrNull(it) } ?: points.last()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(surfaceVariant)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (selectedIndex == null) {
                        "Итог (${activePoint.yearMonth})"
                    } else {
                        "Месяц ${activePoint.monthIndex} (${activePoint.yearMonth})"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatRub(activePoint.endBalance),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "+${formatRub(activePoint.totalInterest)} доход",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = tertiaryColor
                )
                Text(
                    text = "Ставка: ${activePoint.appliedRate}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .pointerInput(points) {
                    detectTapGestures(
                        onPress = { offset ->
                            val idx = ((offset.x / size.width) * (points.size - 1))
                                .roundToInt()
                                .coerceIn(0, points.size - 1)
                            selectedIndex = idx
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            tryAwaitRelease()
                            selectedIndex = null
                        }
                    )
                }
                .pointerInput(points) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val idx = ((offset.x / size.width) * (points.size - 1))
                                .roundToInt()
                                .coerceIn(0, points.size - 1)
                            selectedIndex = idx
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        },
                        onDragEnd = { selectedIndex = null },
                        onDragCancel = { selectedIndex = null },
                        onDrag = { change, _ ->
                            val idx = ((change.position.x / size.width) * (points.size - 1))
                                .roundToInt()
                                .coerceIn(0, points.size - 1)
                            if (idx != selectedIndex) {
                                selectedIndex = idx
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            }
                        }
                    )
                }
        ) {
            val maxVal = points.maxOf { it.endBalance.toFloat() }.coerceAtLeast(1f)
            val w = size.width
            val h = size.height

            fun xOf(i: Int) = (i.toFloat() / (points.size - 1)) * w
            fun yOf(v: Float) = h - (v / maxVal) * (h * 0.92f)

            val totalPath = Path()
            val totalFillPath = Path()
            val investedPath = Path()

            points.forEachIndexed { i, pt ->
                val x = xOf(i)
                val yTotal = yOf(pt.endBalance.toFloat())
                val yInv = yOf(pt.totalInvested.toFloat())

                if (i == 0) {
                    totalPath.moveTo(x, yTotal)
                    totalFillPath.moveTo(x, h)
                    totalFillPath.lineTo(x, yTotal)
                    investedPath.moveTo(x, yInv)
                } else {
                    totalPath.lineTo(x, yTotal)
                    totalFillPath.lineTo(x, yTotal)
                    investedPath.lineTo(x, yInv)
                }
            }
            totalFillPath.lineTo(w, h)
            totalFillPath.close()

            drawPath(
                path = totalFillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(tertiaryColor.copy(alpha = 0.35f), Color.Transparent)
                )
            )

            drawPath(
                path = investedPath,
                color = primaryColor.copy(alpha = 0.7f),
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )

            drawPath(
                path = totalPath,
                color = tertiaryColor,
                style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
            )

            selectedIndex?.let { idx ->
                val sx = xOf(idx)
                val sy = yOf(points[idx].endBalance.toFloat())
                drawLine(
                    color = primaryColor.copy(alpha = 0.5f),
                    start = Offset(sx, 0f),
                    end = Offset(sx, h),
                    strokeWidth = 1.5.dp.toPx()
                )
                drawCircle(
                    color = tertiaryColor,
                    radius = 6.dp.toPx(),
                    center = Offset(sx, sy)
                )
                drawCircle(
                    color = Color.White,
                    radius = 3.dp.toPx(),
                    center = Offset(sx, sy)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LegendDot(color = primaryColor, label = "Свои взносы")
            LegendDot(color = tertiaryColor, label = "Капитал с % (зажми график)")
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(50))
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
