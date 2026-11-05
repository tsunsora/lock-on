package app.lockon.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.unit.dp
import app.lockon.ui.theme.LockOnShapes

/** More drawn icons, for what Material's core icon set lacks. */
object Glyphs {
    private fun icon(name: String, block: PathBuilder.() -> Unit) =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).addPath(PathData(block), fill = SolidColor(Color.Black)).build()

    /** A folder. */
    val Folder: ImageVector = icon("Folder") {
        moveTo(4f, 5f); lineTo(9.5f, 5f); lineTo(11.5f, 7f); lineTo(20f, 7f); quadTo(22f, 7f, 22f, 9f); lineTo(22f, 18f); quadTo(22f, 20f, 20f, 20f)
        lineTo(4f, 20f); quadTo(2f, 20f, 2f, 18f); lineTo(2f, 7f); quadTo(2f, 5f, 4f, 5f); close()
    }

    /** A clipboard. */
    val Paste: ImageVector = icon("Paste") {
        moveTo(9f, 2f); lineTo(15f, 2f); lineTo(15f, 4f); lineTo(18f, 4f); quadTo(20f, 4f, 20f, 6f); lineTo(20f, 20f); quadTo(20f, 22f, 18f, 22f)
        lineTo(6f, 22f); quadTo(4f, 22f, 4f, 20f); lineTo(4f, 6f); quadTo(4f, 4f, 6f, 4f); lineTo(9f, 4f); close()
    }

    /** A shield: Unit, and blockers. */
    val Shield: ImageVector = icon("Shield") {
        moveTo(12f, 2f); lineTo(20f, 5f); lineTo(20f, 11f); quadTo(20f, 18f, 12f, 22f); quadTo(4f, 18f, 4f, 11f); lineTo(4f, 5f); close()
    }

    /** Two links of a chain, joined by a bar. */
    val Link: ImageVector = icon("Link") {
        moveTo(7f, 7f); lineTo(11f, 7f); lineTo(11f, 9f); lineTo(7f, 9f); quadTo(4f, 9f, 4f, 12f); quadTo(4f, 15f, 7f, 15f); lineTo(11f, 15f); lineTo(11f, 17f); lineTo(7f, 17f)
        quadTo(2f, 17f, 2f, 12f); quadTo(2f, 7f, 7f, 7f); close()
        moveTo(13f, 7f); lineTo(17f, 7f); quadTo(22f, 7f, 22f, 12f); quadTo(22f, 17f, 17f, 17f); lineTo(13f, 17f); lineTo(13f, 15f); lineTo(17f, 15f)
        quadTo(20f, 15f, 20f, 12f); quadTo(20f, 9f, 17f, 9f); lineTo(13f, 9f); close()
        moveTo(8f, 11f); lineTo(16f, 11f); lineTo(16f, 13f); lineTo(8f, 13f); close()
    }

    /** A lightning bolt: Command. */
    val Bolt: ImageVector = icon("Bolt") {
        moveTo(13f, 2f); lineTo(5f, 14f); lineTo(11f, 14f); lineTo(10f, 22f); lineTo(19f, 9f); lineTo(13f, 9f); close()
    }

    /** A computer's screen on a stand. */
    val Monitor: ImageVector = icon("Monitor") {
        moveTo(4f, 4f); lineTo(20f, 4f); quadTo(22f, 4f, 22f, 6f); lineTo(22f, 15f); quadTo(22f, 17f, 20f, 17f); lineTo(13f, 17f); lineTo(13f, 19f); lineTo(16f, 19f); lineTo(16f, 21f)
        lineTo(8f, 21f); lineTo(8f, 19f); lineTo(11f, 19f); lineTo(11f, 17f); lineTo(4f, 17f); quadTo(2f, 17f, 2f, 15f); lineTo(2f, 6f); quadTo(2f, 4f, 4f, 4f); close()
        // Winds against the frame, so the screen stays hollow.
        moveTo(4f, 6f); lineTo(4f, 15f); lineTo(20f, 15f); lineTo(20f, 6f); close()
    }

    /** A phone. */
    val Phone: ImageVector = icon("Phone") {
        moveTo(8f, 2f); lineTo(16f, 2f); quadTo(18f, 2f, 18f, 4f); lineTo(18f, 20f); quadTo(18f, 22f, 16f, 22f); lineTo(8f, 22f); quadTo(6f, 22f, 6f, 20f); lineTo(6f, 4f); quadTo(6f, 2f, 8f, 2f); close()
        moveTo(8f, 4f); lineTo(8f, 19f); lineTo(16f, 19f); lineTo(16f, 4f); close()
    }

    /** Three slider lines: filters. */
    val Filters: ImageVector = icon("Filters") {
        moveTo(3f, 5f); lineTo(21f, 5f); lineTo(21f, 7f); lineTo(3f, 7f); close()
        moveTo(6f, 11f); lineTo(18f, 11f); lineTo(18f, 13f); lineTo(6f, 13f); close()
        moveTo(10f, 17f); lineTo(14f, 17f); lineTo(14f, 19f); lineTo(10f, 19f); close()
    }

    /** Shrinking lines beside a down arrow: sort order. */
    val Sort: ImageVector = icon("Sort") {
        moveTo(6f, 4f); lineTo(8f, 4f); lineTo(8f, 16f); lineTo(11f, 13f); lineTo(12.4f, 14.4f); lineTo(7f, 20f); lineTo(1.6f, 14.4f); lineTo(3f, 13f); lineTo(6f, 16f); close()
        moveTo(13f, 5f); lineTo(22f, 5f); lineTo(22f, 7f); lineTo(13f, 7f); close()
        moveTo(13f, 10f); lineTo(19f, 10f); lineTo(19f, 12f); lineTo(13f, 12f); close()
        moveTo(13f, 15f); lineTo(16f, 15f); lineTo(16f, 17f); lineTo(13f, 17f); close()
    }

    /** A grid of four: whole cards. */
    val Grid: ImageVector = icon("Grid") {
        moveTo(3f, 3f); lineTo(11f, 3f); lineTo(11f, 11f); lineTo(3f, 11f); close()
        moveTo(13f, 3f); lineTo(21f, 3f); lineTo(21f, 11f); lineTo(13f, 11f); close()
        moveTo(3f, 13f); lineTo(11f, 13f); lineTo(11f, 21f); lineTo(3f, 21f); close()
        moveTo(13f, 13f); lineTo(21f, 13f); lineTo(21f, 21f); lineTo(13f, 21f); close()
    }

    /** Lines of a list. */
    val Rows: ImageVector = icon("Rows") {
        moveTo(3f, 5f); lineTo(21f, 5f); lineTo(21f, 7.5f); lineTo(3f, 7.5f); close()
        moveTo(3f, 10.75f); lineTo(21f, 10.75f); lineTo(21f, 13.25f); lineTo(3f, 13.25f); close()
        moveTo(3f, 16.5f); lineTo(21f, 16.5f); lineTo(21f, 19f); lineTo(3f, 19f); close()
    }

    /** A single card on its side: the side deck. */
    val SideCard: ImageVector = icon("SideCard") {
        moveTo(8f, 3f); lineTo(16f, 3f); quadTo(18f, 3f, 18f, 5f); lineTo(18f, 19f); quadTo(18f, 21f, 16f, 21f); lineTo(8f, 21f); quadTo(6f, 21f, 6f, 19f); lineTo(6f, 5f); quadTo(6f, 3f, 8f, 3f); close()
    }

    /** An arrow turning back: restore. */
    val Undo: ImageVector = icon("Undo") {
        moveTo(9f, 4f); lineTo(10.4f, 5.4f); lineTo(7.8f, 8f); lineTo(14f, 8f); quadTo(21f, 8f, 21f, 14f); quadTo(21f, 20f, 14f, 20f); lineTo(8f, 20f); lineTo(8f, 18f); lineTo(14f, 18f)
        quadTo(19f, 18f, 19f, 14f); quadTo(19f, 10f, 14f, 10f); lineTo(7.8f, 10f); lineTo(10.4f, 12.6f); lineTo(9f, 14f); lineTo(4f, 9f); close()
    }
}

/** A deck's state as an outlined tag: Legal with a check, Needs review in red with a warning, Draft plain. */
@Composable
fun StateTag(legal: Boolean, review: Boolean = false, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val ink = if (review) scheme.error else scheme.onSurface
    val shape = RoundedCornerShape(9.dp)
    Row(
        modifier.clip(shape).border(1.dp, if (review) scheme.error else scheme.onSurface.copy(alpha = 0.2f), shape).padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (review) Icon(Icons.Default.Warning, contentDescription = null, Modifier.size(14.dp), tint = ink)
        else if (legal) Icon(Icons.Default.Check, contentDescription = null, Modifier.size(14.dp), tint = ink)
        Text(if (review) "Needs review" else if (legal) "Legal" else "Draft", style = MaterialTheme.typography.labelMedium, color = ink, maxLines = 1, softWrap = false)
    }
}

/** A quiet tile on a panel: a soft fill rather than another card with an edge. */
@Composable
fun SoftTile(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.clip(LockOnShapes.tile).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp), content = content,
    )
}
