package sh.comfy.waves.launcher.search

import android.app.SearchManager
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sh.comfy.waves.launcher.data.AppInfo
import sh.comfy.waves.launcher.data.AppRepository
import sh.comfy.waves.launcher.drawer.AppGridItem

/**
 * Full-screen search overlay.
 * Shows app results + option to web search.
 * Nova-style: type-to-filter with app suggestions.
 */
@Composable
fun SearchOverlay(
    visible: Boolean,
    apps: List<AppInfo>,
    onAppClick: (AppInfo) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)) + slideInVertically(
            initialOffsetY = { -it / 4 },
            animationSpec = tween(250),
        ),
        exit = fadeOut(tween(150)) + slideOutVertically(
            targetOffsetY = { -it / 4 },
            animationSpec = tween(200),
        ),
        modifier = modifier,
    ) {
        val context = LocalContext.current
        var query by remember { mutableStateOf("") }
        val focusRequester = remember { FocusRequester() }

        // Auto-focus search field
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }

        val filteredApps = AppRepository.searchApps(apps, query).take(12)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.97f))
                .clickable { onDismiss() },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clickable(enabled = false) { /* block click-through */ },
            ) {
                // Search input
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 20.sp,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            if (query.isNotBlank()) {
                                // Web search fallback
                                val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                                    putExtra(SearchManager.QUERY, query)
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    // Fallback to browser
                                    val browserIntent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")
                                    ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                                    context.startActivity(browserIntent)
                                }
                                onDismiss()
                            }
                        }
                    ),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clip(RoundedCornerShape(28.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 24.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            if (query.isEmpty()) {
                                Text(
                                    "Search apps & web...",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 20.sp,
                                )
                            }
                            innerTextField()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                )

                // App results
                if (filteredApps.isNotEmpty()) {
                    Text(
                        "Apps",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp, start = 4.dp),
                    )

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        items(filteredApps) { app ->
                            SearchResultItem(
                                app = app,
                                onClick = {
                                    onAppClick(app)
                                    onDismiss()
                                },
                            )
                        }
                    }
                }

                // Web search suggestion
                if (query.isNotBlank()) {
                    Text(
                        "Search web for \"$query\"",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 15.sp,
                        modifier = Modifier
                            .padding(top = 16.dp, start = 4.dp)
                            .clickable {
                                val browserIntent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")
                                ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                                context.startActivity(browserIntent)
                                onDismiss()
                            },
                    )
                }
            }
        }
    }
}

@Composable
fun SearchResultItem(
    app: AppInfo,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AppGridItem(
            app = app,
            iconSize = 40.dp,
            showLabel = false,
            onClick = onClick,
            onLongClick = {},
        )
        Text(
            text = app.label,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp,
        )
    }
}
