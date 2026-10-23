package com.onlasdan.comic.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.onlasdan.comic.R
import com.onlasdan.comic.ui.components.PrimaryButton
import com.onlasdan.comic.ui.components.SecondaryButton
import com.onlasdan.comic.ui.theme.CardShape
import com.onlasdan.comic.ui.theme.DarkOutline
import com.onlasdan.comic.ui.theme.DarkSurface
import com.onlasdan.comic.ui.theme.PureBlack
import com.onlasdan.comic.ui.theme.Spacing
import com.onlasdan.comic.ui.theme.StrokeWidth
import com.onlasdan.comic.ui.theme.TextPrimary
import com.onlasdan.comic.ui.theme.TextSecondary
import com.onlasdan.comic.ui.util.BackupSummary

// Halaman penyimpanan (onboarding + tawaran pulih) — diekstrak dari
// MainActivity pada Fase B1 agar file itu kembali di bawah cap bekunya;
// kerangka kartu bersama agar tiap halaman ringkas.
@Composable
private fun StoragePageShell(
    title: String,
    body: String,
    actions: @Composable () -> Unit,
) {
    Scaffold(containerColor = PureBlack) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(Spacing.xl),
            verticalArrangement = Arrangement.Center,
        ) {
            Surface(color = DarkSurface, shape = CardShape, border = androidx.compose.foundation.BorderStroke(StrokeWidth.regular, DarkOutline)) {
                Column(modifier = Modifier.padding(Spacing.xl), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    Text(text = title, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    Text(text = body, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    actions()
                }
            }
        }
    }
}

@Composable
fun StorageOnboardingPage(onEnable: () -> Unit, onSkip: () -> Unit) {
    StoragePageShell(title = stringResource(R.string.storage_onboarding_title), body = stringResource(R.string.storage_onboarding_body)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md), modifier = Modifier.fillMaxWidth()) {
            SecondaryButton(text = stringResource(R.string.storage_onboarding_no), onClick = onSkip, modifier = Modifier.weight(1f))
            PrimaryButton(text = stringResource(R.string.storage_onboarding_yes), onClick = onEnable, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun StorageRestorePage(summary: BackupSummary?, onRestore: () -> Unit, onSkip: () -> Unit) {
    val bodyText = if (summary != null && (summary.historyCount > 0L || summary.bookmarksCount > 0L)) {
        pluralStringResource(R.plurals.storage_restore_with_counts, summary.historyCount.toInt(), summary.historyCount, summary.bookmarksCount)
    } else {
        stringResource(R.string.storage_restore_body)
    }
    StoragePageShell(title = stringResource(R.string.storage_restore_title), body = bodyText) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md), modifier = Modifier.fillMaxWidth()) {
            SecondaryButton(text = stringResource(R.string.storage_restore_skip), onClick = onSkip, modifier = Modifier.weight(1f))
            PrimaryButton(text = stringResource(R.string.storage_restore_confirm), onClick = onRestore, modifier = Modifier.weight(1f))
        }
    }
}
