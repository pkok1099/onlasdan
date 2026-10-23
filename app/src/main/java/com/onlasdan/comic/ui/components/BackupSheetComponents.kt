package com.onlasdan.comic.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onlasdan.comic.ui.theme.PrimaryPink
import com.onlasdan.comic.ui.theme.Spacing

// Isi tombol backup/restore di ProfileBackupSheet: ikon + label, atau
// spinner saat operasi tombol itu sedang berjalan (Fase A UX).
@Composable
fun BackupButtonContent(icon: ImageVector, text: String, iconTint: Color, textColor: Color, fontWeight: FontWeight, loading: Boolean = false) {
    if (loading) {
        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = iconTint, strokeWidth = 2.dp)
    } else {
        Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
    }
    Spacer(Modifier.width(6.dp))
    Text(text = text, style = MaterialTheme.typography.bodySmall, fontWeight = fontWeight, color = textColor)
}

@Composable
fun SheetSectionTitle(text: String, padded: Boolean = false) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = PrimaryPink,
        letterSpacing = 1.sp,
        modifier = if (padded) Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs) else Modifier,
    )
}
