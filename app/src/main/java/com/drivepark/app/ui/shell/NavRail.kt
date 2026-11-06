package com.drivepark.app.ui.shell

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.drivepark.app.ui.theme.Pleos
import com.drivepark.app.ui.theme.pressScale

enum class RailItem(val route: String, val label: String, val icon: ImageVector) {
    PARK("park", "공원", Icons.Filled.Home),
    FRIENDS("friends", "친구", Icons.Filled.Person),
    COLLECTION("collection", "도감", Icons.Filled.Star),
    SETTINGS("settings", "설정", Icons.Filled.Settings),
}

private val ITEM_HEIGHT = 104.dp

/** 왼쪽 세로 메뉴. 선택 표시 막대가 스프링으로 미끄러진다. */
@Composable
fun NavRail(selected: RailItem?, newFriends: Int, onSelect: (RailItem) -> Unit, modifier: Modifier = Modifier) {
    val index = selected?.ordinal ?: -1
    val barOffset by animateDpAsState(
        ITEM_HEIGHT * index.coerceAtLeast(0) + 24.dp,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "railBar",
    )
    val width = if (com.drivepark.app.ui.theme.LocalCompact.current) 104.dp else Pleos.RailWidth
    Box(modifier.width(width).fillMaxHeight().background(Pleos.Surface)) {
        if (index >= 0) {
            Box(
                Modifier.offset(y = barOffset + 20.dp).padding(start = 8.dp)
                    .size(width = 6.dp, height = ITEM_HEIGHT - 40.dp)
                    .background(Pleos.Accent, RoundedCornerShape(3.dp)),
            )
        }
        Column(Modifier.padding(top = 24.dp).fillMaxWidth()) {
            RailItem.entries.forEach { item ->
                val interaction = remember { MutableInteractionSource() }
                val isSelected = item == selected
                Column(
                    Modifier.fillMaxWidth().height(ITEM_HEIGHT).pressScale(interaction)
                        .clickable(interaction, indication = null) { onSelect(item) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    val tint = if (isSelected) Pleos.Text else Pleos.TextSecondary
                    BadgedBox(badge = {
                        if (item == RailItem.FRIENDS && newFriends > 0) Badge(containerColor = Pleos.Accent) { Text("$newFriends") }
                    }) {
                        Icon(item.icon, contentDescription = item.label, tint = tint, modifier = Modifier.size(36.dp))
                    }
                    Text(item.label, color = tint, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 6.dp))
                }
            }
        }
    }
}
