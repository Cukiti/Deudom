package com.deudom.deudom.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.deudom.deudom.data.Loan
import com.deudom.deudom.ui.theme.GoldPremium
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SimpleBarChart(loans: List<Loan>) {
    val monthlyData = (0..5).map { i ->
        val cal = Calendar.getInstance().apply { add(Calendar.MONTH, -i) }
        val month = cal.get(Calendar.MONTH); val year = cal.get(Calendar.YEAR)
        val amount = loans.filter { val lCal = Calendar.getInstance().apply { timeInMillis = it.date }; lCal.get(Calendar.MONTH) == month && lCal.get(Calendar.YEAR) == year }.sumOf { it.amount }
        amount to SimpleDateFormat("MMM", Locale.getDefault()).format(cal.time)
    }.reversed()
    val maxVal = monthlyData.maxOf { it.first }.coerceAtLeast(1.0)
    Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.SpaceEvenly) {
        monthlyData.forEach { (amt, label) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val hp = (amt / maxVal).toFloat().coerceIn(0.05f, 1f)
                Box(modifier = Modifier.width(20.dp).fillMaxHeight(hp * 0.8f).clip(CircleShape).background(GoldPremium))
                Spacer(modifier = Modifier.height(8.dp)); Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}
