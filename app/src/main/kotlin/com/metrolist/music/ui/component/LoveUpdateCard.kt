/**
 * Full-width "update available" card (Play Store style) + bottom sheet with changelog and install.
 */

@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.metrolist.music.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.metrolist.music.R
import com.metrolist.music.utils.LoveInstallState
import com.metrolist.music.utils.LoveUpdateInfo
import com.metrolist.music.utils.LoveUpdater

@Composable
fun LoveUpdateCard(modifier: Modifier = Modifier) {
    val update by LoveUpdater.update.collectAsState()
    LaunchedEffect(Unit) { LoveUpdater.check() }

    var showSheet by remember { mutableStateOf(false) }
    val info = update ?: return

    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = modifier.fillMaxWidth().clickable { showSheet = true },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Icon(painterResource(R.drawable.update), contentDescription = null, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Update available", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "LoveMusic ${info.version} is ready. Tap to see what's new",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Icon(painterResource(R.drawable.navigate_next), contentDescription = null)
        }
    }

    if (showSheet) {
        LoveUpdateSheet(info = info, onDismiss = { showSheet = false })
    }
}

@Composable
private fun LoveUpdateSheet(
    info: LoveUpdateInfo,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val state by LoveUpdater.installState.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 20.dp)) {
            Text("LoveMusic ${info.version}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            val meta =
                listOfNotNull(
                    info.date.takeIf { it.isNotBlank() },
                    info.apkSize.takeIf { it > 0 }?.let { "%.1f MB".format(it / 1048576f) },
                ).joinToString("  •  ")
            if (meta.isNotEmpty()) {
                Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(16.dp))
            Text("What's new", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
            ) {
                MarkdownLines(info.changelog.ifBlank { "Bug fixes and improvements." })
            }
            Spacer(Modifier.height(16.dp))
            when (val s = state) {
                is LoveInstallState.Downloading -> {
                    LinearProgressIndicator(progress = { s.progress }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(6.dp))
                    Text("Downloading… ${(s.progress * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                }
                is LoveInstallState.Ready ->
                    Button(onClick = { LoveUpdater.install(context, s.file) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Install")
                    }
                is LoveInstallState.Error -> {
                    Text(s.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(6.dp))
                    Button(onClick = { LoveUpdater.startDownload(context) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Retry")
                    }
                }
                LoveInstallState.Idle ->
                    Button(onClick = { LoveUpdater.startDownload(context) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Update")
                    }
            }
        }
    }
}

/** Tiny markdown renderer: headings, bullets, **bold**, `code`. Enough for a changelog README. */
@Composable
private fun MarkdownLines(text: String) {
    text.lines().forEach { raw ->
        val line = raw.trimEnd()
        when {
            line.isBlank() -> Spacer(Modifier.height(2.dp))
            line.startsWith("#") -> {
                val level = line.takeWhile { it == '#' }.length
                Text(
                    inline(line.dropWhile { it == '#' }.trim()),
                    style = if (level <= 1) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            line.trimStart().startsWith("- ") || line.trimStart().startsWith("* ") ->
                Row {
                    Text("•  ", style = MaterialTheme.typography.bodyMedium)
                    Text(inline(line.trimStart().drop(2)), style = MaterialTheme.typography.bodyMedium)
                }
            else -> Text(inline(line), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun inline(s: String): AnnotatedString =
    buildAnnotatedString {
        val clean = s.replace("`", "")
        var i = 0
        while (i < clean.length) {
            if (clean.startsWith("**", i)) {
                val end = clean.indexOf("**", i + 2)
                if (end > 0) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(clean.substring(i + 2, end)) }
                    i = end + 2
                    continue
                }
            }
            append(clean[i])
            i++
        }
    }
