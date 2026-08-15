package app.nudroidlabs.waktusolat.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.nudroidlabs.waktusolat.doa.DoaCatalog
import app.nudroidlabs.waktusolat.doa.DoaEntry

@Composable
fun DoaScreen(modifier: Modifier) {
    var expandedId by rememberSaveable { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Column {
                Text("Doa Harian", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Arab · Rumi · maksud ringkas · rujukan",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 13.sp
                )
            }
        }

        items(DoaCatalog.entries, key = DoaEntry::id) { doa ->
            DoaCard(
                doa = doa,
                expanded = expandedId == doa.id,
                onToggle = {
                    expandedId = if (expandedId == doa.id) null else doa.id
                }
            )
        }

        item {
            Text(
                "Rujukan setiap doa dinyatakan pada kad. Maksud Bahasa Melayu ialah " +
                    "maksud ringkas untuk bacaan mudah.",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )
        }
    }
}

@Composable
private fun DoaCard(
    doa: DoaEntry,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 13.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        doa.category,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        doa.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    if (expanded) "▲" else "▼",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 13.sp
                )
            }

            if (expanded) {
                Spacer(Modifier.height(14.dp))
                Text(
                    doa.arabic,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                    fontSize = 25.sp,
                    lineHeight = 40.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(Modifier.height(12.dp))
                Text(
                    "Rumi",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(doa.rumi, fontSize = 14.sp)

                Spacer(Modifier.height(10.dp))
                Text(
                    "Maksud",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(doa.meaningMalay, fontSize = 14.sp)

                Spacer(Modifier.height(10.dp))
                Text(
                    "Rujukan · ${doa.reference}",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                    fontSize = 11.sp
                )
            }
        }
    }
}
