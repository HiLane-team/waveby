package com.drivepark.app.ui.collection

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.drivepark.app.pleos.PleosLocationProvider
import com.drivepark.app.ui.character.MascotAvatar

@Composable
fun CollectionScreen(vm: CollectionViewModel) {
    val state by vm.state.collectAsState()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { vm.refreshPermission() }
    LaunchedEffect(Unit) { vm.refreshPermission() }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("지역 캐릭터 도감", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.weight(1f))
            Text("${state.items.count { it.second }} / ${state.items.size}", style = MaterialTheme.typography.titleLarge)
        }
        if (!state.locationGranted) {
            Card(
                Modifier.fillMaxWidth().padding(vertical = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("위치를 허용하면 지역 캐릭터를 모을 수 있어요", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    Button(onClick = { launcher.launch(PleosLocationProvider.PERMISSION) }) { Text("위치 허용") }
                }
            }
        }
        LazyVerticalGrid(
            GridCells.Fixed(2), Modifier.fillMaxSize().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(state.items, key = { it.first.id }) { (region, collected) ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        MascotAvatar(region.id, Modifier.size(88.dp), silhouette = !collected, sameDot = false)
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(if (collected) region.mascotName else "???", style = MaterialTheme.typography.titleLarge)
                            Text(region.name, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
