package app.nudroidlabs.waktusolat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.nudroidlabs.waktusolat.data.PrayerResponse

@Composable
fun WeekScreen(
    modifier: Modifier,
    data: PrayerResponse?,
    loading: Boolean,
    error: String?,
    onRefresh: () -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Jadual 7 Hari", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(
                "Jadual semasa daripada e-Solat JAKIM",
                color = MaterialTheme.colorScheme.primary
            )
        }

        when {
            loading -> item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            error != null -> item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    androidx.compose.foundation.layout.Column(Modifier.padding(18.dp)) {
                        Text("Data tidak dapat dimuatkan", fontWeight = FontWeight.Bold)
                        Text(error)
                        Button(onClick = onRefresh) { Text("Cuba lagi") }
                    }
                }
            }

            data != null -> {
                items(data.days, key = { it.dateRaw }) { day ->
                    PrayerTimesCard(
                        day = day,
                        title = day.dayRaw.ifBlank { "Jadual" },
                        compact = true
                    )
                }
            }
        }
    }
}
