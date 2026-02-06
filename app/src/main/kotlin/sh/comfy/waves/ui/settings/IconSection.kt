package sh.comfy.waves.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import sh.comfy.waves.R
import sh.comfy.waves.iconpack.IconPackHelper

@Composable
fun IconSection(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    // Discover icon drawables from the icon pack
    // For now, we have only the placeholder. As icons are added, this list grows.
    val iconResIds = remember { listOf(R.drawable.ic_placeholder) }
    val hasIcons = iconResIds.size > 1 // More than just the placeholder

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_icons_section),
            style = MaterialTheme.typography.titleLarge,
        )

        if (!hasIcons) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Text(
                    text = stringResource(R.string.no_icons_yet),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                )
            }
        } else {
            // Icon preview grid — icons are tinted with Material You primary color
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 56.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(iconResIds) { resId ->
                    Icon(
                        painter = painterResource(resId),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp),
                    )
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Button(
                onClick = { IconPackHelper.applyToNiagara(context) },
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.apply_niagara))
            }

            OutlinedButton(
                onClick = { IconPackHelper.applyToLauncher(context) },
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.apply_launcher))
            }
        }
    }
}
