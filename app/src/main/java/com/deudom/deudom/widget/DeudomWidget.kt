package com.deudom.deudom.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.deudom.deudom.MainActivity
import com.deudom.deudom.util.Prefs
import com.deudom.deudom.util.formatCurrency
import com.deudom.deudom.R
import com.deudom.deudom.ui.theme.GoldPremium

class DeudomWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Read data from Prefs (no suspend needed as Prefs is synchronous SharedPreferences)
        val balance = Prefs.getCachedBalance(context)
        val currency = Prefs.getCachedCurrency(context)
        
        provideContent {
            DeudomWidgetContent(balance, currency)
        }
    }

    @Composable
    fun DeudomWidgetContent(balance: Double, currency: String) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(Color(0xFF0A192F))) // NavyDeep
                .padding(16.dp)
                .clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.Vertical.CenterVertically,
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally
        ) {
            Text(
                text = "DEUDOM",
                style = TextStyle(
                    color = ColorProvider(Color(0xFFD4AF37)), // GoldPremium
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            )
            Spacer(modifier = GlanceModifier.height(8.dp))
            Text(
                text = formatCurrency(balance, currency),
                style = TextStyle(
                    color = ColorProvider(Color.White),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = "Disponible",
                style = TextStyle(
                    color = ColorProvider(Color(0xFF8892B0)), // GreySecondary
                    fontSize = 10.sp
                )
            )
        }
    }
}
