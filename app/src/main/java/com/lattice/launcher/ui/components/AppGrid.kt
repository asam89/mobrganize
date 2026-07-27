package com.lattice.launcher.ui.components

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lattice.launcher.data.HomeLayout

@Composable
fun AppGrid(
    layout: HomeLayout,
    onAppClick: (String) -> Unit,
    onAppLongClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = modifier,
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (category in layout.categories) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp, horizontal = 4.dp)
                )
            }
            items(category.packageNames) { packageName ->
                AppIcon(
                    packageName = packageName,
                    context = context,
                    onClick = { onAppClick(packageName) },
                    onLongClick = { onAppLongClick(packageName) }
                )
            }
        }
    }
}

private fun drawableToBitmap(drawable: Drawable): Bitmap {
    val width = drawable.intrinsicWidth.coerceAtLeast(1)
    val height = drawable.intrinsicHeight.coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppIcon(
    packageName: String,
    context: Context,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val pm = context.packageManager
    val icon: Drawable? = try {
        pm.getApplicationIcon(packageName)
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }
    val label = try {
        pm.getApplicationInfo(packageName, 0).loadLabel(pm).toString()
    } catch (_: PackageManager.NameNotFoundException) {
        packageName.substringAfterLast('.')
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(4.dp)
    ) {
        if (icon != null) {
            val bitmap = remember(packageName) { drawableToBitmap(icon).asImageBitmap() }
            Image(
                bitmap = bitmap,
                contentDescription = label,
                modifier = Modifier.size(48.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
