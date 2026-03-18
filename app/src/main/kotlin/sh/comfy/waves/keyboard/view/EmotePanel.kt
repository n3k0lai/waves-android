package sh.comfy.waves.keyboard.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import sh.comfy.waves.keyboard.emoji.EmoteRepository

/**
 * Full emote panel: search bar + recently used + full grid.
 * Each emote tap commits its name as text (e.g. "KEKW")
 * and pastes the emote image to clipboard for apps that support it.
 */
@Composable
fun EmotePanel(
    emoteRepo: EmoteRepository,
    onEmoteSelected: (EmoteRepository.Emote) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val allEmotes by emoteRepo.emotes.collectAsState()
    val isLoading by emoteRepo.isLoading.collectAsState()
    val recentNames by emoteRepo.recentEmotes.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    // Fetch emotes on first display
    LaunchedEffect(Unit) {
        if (allEmotes.isEmpty()) {
            emoteRepo.refresh()
        }
    }

    val displayEmotes = if (searchQuery.isBlank()) allEmotes else emoteRepo.search(searchQuery)
    val recentEmotes = emoteRepo.getRecentEmoteObjects()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
            .background(MaterialTheme.colorScheme.surface),
    ) {
        // Top bar: search + ABC button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Search field
            BasicTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                singleLine = true,
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                "Search emotes...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp,
                            )
                        }
                        innerTextField()
                    }
                },
                modifier = Modifier.weight(1f),
            )

            // Back to keyboard button
            Box(
                modifier = Modifier
                    .height(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable { onBack() }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "ABC",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        if (isLoading && allEmotes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp),
                )
            }
        } else {
            // Emote grid
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 44.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                // Recently used section
                if (searchQuery.isBlank() && recentEmotes.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            "Recently Used",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 2.dp),
                        )
                    }
                    items(recentEmotes, key = { "recent_${it.name}" }) { emote ->
                        EmoteCell(emote = emote, onClick = { onEmoteSelected(emote) })
                    }
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            "All Emotes (${allEmotes.size})",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 2.dp),
                        )
                    }
                }

                items(displayEmotes, key = { it.name }) { emote ->
                    EmoteCell(emote = emote, onClick = { onEmoteSelected(emote) })
                }

                if (displayEmotes.isEmpty() && !isLoading) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            if (searchQuery.isNotBlank()) "No emotes matching \"$searchQuery\""
                            else "No emotes loaded",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmoteCell(
    emote: EmoteRepository.Emote,
    onClick: () -> Unit,
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(4.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(emote.url)
                .crossfade(true)
                .memoryCacheKey(emote.name)
                .build(),
            contentDescription = emote.name,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(36.dp)
                .padding(2.dp),
        )
    }
}
