package app.lockon.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** The app's outline icons, on a 24-unit grid with a 1.9-unit round stroke, tinted like any other icon. */
object LineIcons {
    private fun icon(name: String, vararg lines: String, solid: String = ""): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            lines.forEach { addPath(addPathNodes(it), stroke = SolidColor(Color.Black), strokeLineWidth = 1.9f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) }
            if (solid.isNotEmpty()) addPath(addPathNodes(solid), fill = SolidColor(Color.Black))
        }.build()

    val Decks: ImageVector by lazy { icon("Decks", "M5.3 8.5H12.7A1.8 1.8 0 0 1 14.5 10.3V19.7A1.8 1.8 0 0 1 12.7 21.5H5.3A1.8 1.8 0 0 1 3.5 19.7V10.3A1.8 1.8 0 0 1 5.3 8.5Z", "M7 8.5V6.3A1.8 1.8 0 0 1 8.8 4.5h7.4A1.8 1.8 0 0 1 18 6.3v9.4A1.8 1.8 0 0 1 16.2 17.5H14.5", "M10.5 4.5V3.8A1.8 1.8 0 0 1 12.3 2h6.4a1.8 1.8 0 0 1 1.8 1.8v9.4a1.8 1.8 0 0 1-1.8 1.8H18") }
    val Cards: ImageVector by lazy { icon("Cards", "M6.7 2.5H17.3A2.2 2.2 0 0 1 19.5 4.7V19.3A2.2 2.2 0 0 1 17.3 21.5H6.7A2.2 2.2 0 0 1 4.5 19.3V4.7A2.2 2.2 0 0 1 6.7 2.5Z", "M12 7.5l3 4.5-3 4.5-3-4.5z") }
    val Sync: ImageVector by lazy { icon("Sync", "M20 11a8 8 0 0 0-14.3-4.9L4 8", "M4 4v4h4", "M4 13a8 8 0 0 0 14.3 4.9L20 16", "M20 20v-4h-4") }
    val Settings: ImageVector by lazy { icon("Settings", "M9 12A3 3 0 1 1 15 12A3 3 0 1 1 9 12Z", "M19.4 15a1.7 1.7 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-1.8-.3 1.7 1.7 0 0 0-1 1.5V21a2 2 0 1 1-4 0v-.1a1.7 1.7 0 0 0-1.1-1.5 1.7 1.7 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.7 1.7 0 0 0 .3-1.8 1.7 1.7 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.1a1.7 1.7 0 0 0 1.5-1.1 1.7 1.7 0 0 0-.3-1.8l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.7 1.7 0 0 0 1.8.3H9a1.7 1.7 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.1a1.7 1.7 0 0 0 1 1.5 1.7 1.7 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.7 1.7 0 0 0-.3 1.8V9a1.7 1.7 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.1a1.7 1.7 0 0 0-1.5 1z") }
    val Search: ImageVector by lazy { icon("Search", "M4.5 11A6.5 6.5 0 1 1 17.5 11A6.5 6.5 0 1 1 4.5 11Z", "m16 16 4.5 4.5") }
    val Plus: ImageVector by lazy { icon("Plus", "M12 5v14M5 12h14") }
    val Minus: ImageVector by lazy { icon("Minus", "M5 12h14") }
    val Back: ImageVector by lazy { icon("Back", "M19 12H5M11 6l-6 6 6 6") }
    val More: ImageVector by lazy { icon("More", "M10.8 5.5A1.2 1.2 0 1 1 13.2 5.5A1.2 1.2 0 1 1 10.8 5.5Z", "M10.8 12A1.2 1.2 0 1 1 13.2 12A1.2 1.2 0 1 1 10.8 12Z", "M10.8 18.5A1.2 1.2 0 1 1 13.2 18.5A1.2 1.2 0 1 1 10.8 18.5Z") }
    val Export: ImageVector by lazy { icon("Export", "M12 15V3.5M7.5 8 12 3.5 16.5 8", "M5 12.5V19a1.5 1.5 0 0 0 1.5 1.5h11A1.5 1.5 0 0 0 19 19v-6.5") }
    val Check: ImageVector by lazy { icon("Check", "m5 12.5 4.5 4.5L19 7.5") }
    val X: ImageVector by lazy { icon("X", "M6 6l12 12M18 6 6 18") }
    val Folder: ImageVector by lazy { icon("Folder", "M3.5 7.5A2 2 0 0 1 5.5 5.5h4l2 2.5h7a2 2 0 0 1 2 2v7.5a2 2 0 0 1-2 2h-13a2 2 0 0 1-2-2z") }
    val Folderx: ImageVector by lazy { icon("Folderx", "M3.5 7.5A2 2 0 0 1 5.5 5.5h4l2 2.5h7a2 2 0 0 1 2 2v7.5a2 2 0 0 1-2 2h-13a2 2 0 0 1-2-2z") }
    val Folderplus: ImageVector by lazy { icon("Folderplus", "M3.5 7.5A2 2 0 0 1 5.5 5.5h4l2 2.5h7a2 2 0 0 1 2 2v7.5a2 2 0 0 1-2 2h-13a2 2 0 0 1-2-2z", "M12 11v5M9.5 13.5h5") }
    val Filter: ImageVector by lazy { icon("Filter", "M4 6h16M7 12h10M10 18h4") }
    val Sort: ImageVector by lazy { icon("Sort", "M7 4v16M3.5 16.5 7 20l3.5-3.5", "M14 6h7M14 11h5M14 16h3") }
    val List: ImageVector by lazy { icon("List", "M8 6h12M8 12h12M8 18h12", "M3.2 6A0.8 0.8 0 1 1 4.8 6A0.8 0.8 0 1 1 3.2 6Z", "M3.2 12A0.8 0.8 0 1 1 4.8 12A0.8 0.8 0 1 1 3.2 12Z", "M3.2 18A0.8 0.8 0 1 1 4.8 18A0.8 0.8 0 1 1 3.2 18Z") }
    val Grid: ImageVector by lazy { icon("Grid", "M5.5 4H9.5A1.5 1.5 0 0 1 11 5.5V9.5A1.5 1.5 0 0 1 9.5 11H5.5A1.5 1.5 0 0 1 4 9.5V5.5A1.5 1.5 0 0 1 5.5 4Z", "M14.5 4H18.5A1.5 1.5 0 0 1 20 5.5V9.5A1.5 1.5 0 0 1 18.5 11H14.5A1.5 1.5 0 0 1 13 9.5V5.5A1.5 1.5 0 0 1 14.5 4Z", "M5.5 13H9.5A1.5 1.5 0 0 1 11 14.5V18.5A1.5 1.5 0 0 1 9.5 20H5.5A1.5 1.5 0 0 1 4 18.5V14.5A1.5 1.5 0 0 1 5.5 13Z", "M14.5 13H18.5A1.5 1.5 0 0 1 20 14.5V18.5A1.5 1.5 0 0 1 18.5 20H14.5A1.5 1.5 0 0 1 13 18.5V14.5A1.5 1.5 0 0 1 14.5 13Z") }
    val Shuffle: ImageVector by lazy { icon("Shuffle", "M16 4h4v4M20 4l-6.5 6.5M4 20l6-6M16 20h4v-4M20 20l-5-5M4 4l5 5") }
    val Draw: ImageVector by lazy { icon("Draw", "M8 3.5H16A2 2 0 0 1 18 5.5V18.5A2 2 0 0 1 16 20.5H8A2 2 0 0 1 6 18.5V5.5A2 2 0 0 1 8 3.5Z", "M12 9v6M9 12h6") }
    val Redo: ImageVector by lazy { icon("Redo", "M20 12a8 8 0 1 1-2.3-5.7L20 8.5", "M20 4v4.5h-4.5") }
    val Clock: ImageVector by lazy { icon("Clock", "M3.5 12A8.5 8.5 0 1 1 20.5 12A8.5 8.5 0 1 1 3.5 12Z", "M12 7.5V12l3 2") }
    val Desktop: ImageVector by lazy { icon("Desktop", "M5 4.5H19A2 2 0 0 1 21 6.5V14.5A2 2 0 0 1 19 16.5H5A2 2 0 0 1 3 14.5V6.5A2 2 0 0 1 5 4.5Z", "M8.5 20h7M12 16.5V20") }
    val Phone: ImageVector by lazy { icon("Phone", "M9 2.5H15A2.5 2.5 0 0 1 17.5 5V19A2.5 2.5 0 0 1 15 21.5H9A2.5 2.5 0 0 1 6.5 19V5A2.5 2.5 0 0 1 9 2.5Z", "M11 18.5h2") }
    val Tablet: ImageVector by lazy { icon("Tablet", "M6.5 3H17.5A2.5 2.5 0 0 1 20 5.5V18.5A2.5 2.5 0 0 1 17.5 21H6.5A2.5 2.5 0 0 1 4 18.5V5.5A2.5 2.5 0 0 1 6.5 3Z", "M11 18h2") }
    val Qr: ImageVector by lazy { icon("Qr", "M5 4H9A1 1 0 0 1 10 5V9A1 1 0 0 1 9 10H5A1 1 0 0 1 4 9V5A1 1 0 0 1 5 4Z", "M15 4H19A1 1 0 0 1 20 5V9A1 1 0 0 1 19 10H15A1 1 0 0 1 14 9V5A1 1 0 0 1 15 4Z", "M5 14H9A1 1 0 0 1 10 15V19A1 1 0 0 1 9 20H5A1 1 0 0 1 4 19V15A1 1 0 0 1 5 14Z", "M14 14h2v2h-2zM18 18h2v2h-2zM18 14h2M14 18v2") }
    val Wifi: ImageVector by lazy { icon("Wifi", "M2.5 9a14 14 0 0 1 19 0M5.5 12.5a9.5 9.5 0 0 1 13 0M8.8 16a5 5 0 0 1 6.4 0", "M11.1 19.2A0.9 0.9 0 1 1 12.9 19.2A0.9 0.9 0 1 1 11.1 19.2Z") }
    val Chev: ImageVector by lazy { icon("Chev", "m9 6 6 6-6 6") }
    val Chevd: ImageVector by lazy { icon("Chevd", "m6 9 6 6 6-6") }
    val Chevu: ImageVector by lazy { icon("Chevu", "m6 15 6-6 6 6") }
    /** Four corner brackets, like the reticle's: show something at full size. */
    val Expand: ImageVector by lazy { icon("Expand", "M4 9V4h5M15 4h5v5M20 15v5h-5M9 20H4v-5") }
    val Trash: ImageVector by lazy { icon("Trash", "M4 7h16M9.5 7V4.5h5V7M6.5 7l.8 12.2a1.5 1.5 0 0 0 1.5 1.3h6.4a1.5 1.5 0 0 0 1.5-1.3L17.5 7") }
    val Edit: ImageVector by lazy { icon("Edit", "M4 20h4L19 9a2.8 2.8 0 0 0-4-4L4 16z") }
    val Undo: ImageVector by lazy { icon("Undo", "M9 14 4 9l5-5", "M4 9h10.5a5.5 5.5 0 0 1 0 11H11") }
    val Image: ImageVector by lazy { icon("Image", "M5.5 4.5H18.5A2 2 0 0 1 20.5 6.5V17.5A2 2 0 0 1 18.5 19.5H5.5A2 2 0 0 1 3.5 17.5V6.5A2 2 0 0 1 5.5 4.5Z", "M7.4 10A1.6 1.6 0 1 1 10.6 10A1.6 1.6 0 1 1 7.4 10Z", "m20.5 16-5-5-9 8.5") }
    val Text: ImageVector by lazy { icon("Text", "M5 6h14M5 11h14M5 16h9") }
    val Print: ImageVector by lazy { icon("Print", "M7 9V3.5h10V9", "M5.5 9H18.5A2 2 0 0 1 20.5 11V15A2 2 0 0 1 18.5 17H5.5A2 2 0 0 1 3.5 15V11A2 2 0 0 1 5.5 9Z", "M7 14h10v6.5H7z") }
    val Copy: ImageVector by lazy { icon("Copy", "M10 8H18A2 2 0 0 1 20 10V18A2 2 0 0 1 18 20H10A2 2 0 0 1 8 18V10A2 2 0 0 1 10 8Z", "M16 8V5.5A1.5 1.5 0 0 0 14.5 4h-9A1.5 1.5 0 0 0 4 5.5v9A1.5 1.5 0 0 0 5.5 16H8") }
    val Save: ImageVector by lazy { icon("Save", "M12 4v11M7.5 10.5 12 15l4.5-4.5", "M5 19.5h14") }
    val Wall: ImageVector by lazy { icon("Wall", "M3.5 6.5h17v11h-17zM3.5 12h17M9 6.5V12M15 6.5V12M6 12v5.5M12 12v5.5M18 12v5.5") }
    val Shield: ImageVector by lazy { icon("Shield", "M12 3.5 19 6v5.5c0 4.4-3 7.7-7 9-4-1.3-7-4.6-7-9V6z") }
    val Burst: ImageVector by lazy { icon("Burst", "M12 3v4M12 17v4M3 12h4M17 12h4M5.6 5.6l2.8 2.8M15.6 15.6l2.8 2.8M5.6 18.4l2.8-2.8M15.6 8.4l2.8-2.8") }
    val Link: ImageVector by lazy { icon("Link", "M10 14a4 4 0 0 0 5.7 0l3-3a4 4 0 0 0-5.7-5.7l-1 1", "M14 10a4 4 0 0 0-5.7 0l-3 3a4 4 0 0 0 5.7 5.7l1-1") }
    val Unit: ImageVector by lazy { icon("Unit", "M12 3 5 7v6l7 8 7-8V7z") }
    val Pilot: ImageVector by lazy { icon("Pilot", "M8 8A4 4 0 1 1 16 8A4 4 0 1 1 8 8Z", "M4.5 20.5a7.5 7.5 0 0 1 15 0") }
    val Command: ImageVector by lazy { icon("Command", "M13 3 5 13.5h6L10 21l8-10.5h-6z") }
    val Base: ImageVector by lazy { icon("Base", "M4 20.5h16M6 20.5V10l6-5.5 6 5.5v10.5M10 20.5v-5h4v5") }
    val Mark: ImageVector by lazy { icon("Mark", "M5.6 12A6.4 6.4 0 1 1 18.4 12A6.4 6.4 0 1 1 5.6 12Z", "M12 3v4M12 17v4M3 12h4M17 12h4", solid = "M9.9 12A2.1 2.1 0 1 1 14.1 12A2.1 2.1 0 1 1 9.9 12Z") }
    val Min: ImageVector by lazy { icon("Min", "M6 12h12") }
    val Max: ImageVector by lazy { icon("Max", "M8.5 6H15.5A2.5 2.5 0 0 1 18 8.5V15.5A2.5 2.5 0 0 1 15.5 18H8.5A2.5 2.5 0 0 1 6 15.5V8.5A2.5 2.5 0 0 1 8.5 6Z") }
    val Swap: ImageVector by lazy { icon("Swap", "M7 4 3.5 7.5 7 11M3.5 7.5H17M17 13l3.5 3.5L17 20M20.5 16.5H7") }
    val Trophy: ImageVector by lazy { icon("Trophy", "M8 4h8v5a4 4 0 0 1-8 0zM8 6H4.5v1.5A3.5 3.5 0 0 0 8 11M16 6h3.5v1.5A3.5 3.5 0 0 1 16 11M12 13v4M8.5 20.5h7M10 17h4v3.5h-4z") }
    val Eye: ImageVector by lazy { icon("Eye", "M2.5 12S6 5.5 12 5.5 21.5 12 21.5 12 18 18.5 12 18.5 2.5 12 2.5 12z", "M9.2 12A2.8 2.8 0 1 1 14.8 12A2.8 2.8 0 1 1 9.2 12Z") }
    val Palette: ImageVector by lazy { icon("Palette", "M12 3.5a8.5 8.5 0 1 0 0 17c1.4 0 1.8-1.1 1.2-2.1-.7-1.1 0-2.4 1.3-2.4H17a3.5 3.5 0 0 0 3.5-3.5c0-5-3.8-9-8.5-9z", "M6.5 11A1 1 0 1 1 8.5 11A1 1 0 1 1 6.5 11Z", "M9 7.5A1 1 0 1 1 11 7.5A1 1 0 1 1 9 7.5Z", "M13.5 7.5A1 1 0 1 1 15.5 7.5A1 1 0 1 1 13.5 7.5Z") }
    val Sliders: ImageVector by lazy { icon("Sliders", "M4 7h9M17 7h3M4 17h3M11 17h9", "M13 7A2 2 0 1 1 17 7A2 2 0 1 1 13 7Z", "M7 17A2 2 0 1 1 11 17A2 2 0 1 1 7 17Z") }
    val Database: ImageVector by lazy { icon("Database", "M4.5 6A7.5 2.8 0 1 1 19.5 6A7.5 2.8 0 1 1 4.5 6Z", "M4.5 6v12c0 1.5 3.4 2.8 7.5 2.8s7.5-1.3 7.5-2.8V6M4.5 12c0 1.5 3.4 2.8 7.5 2.8s7.5-1.3 7.5-2.8") }
    val Info: ImageVector by lazy { icon("Info", "M3.5 12A8.5 8.5 0 1 1 20.5 12A8.5 8.5 0 1 1 3.5 12Z", "M12 11v5.5M12 7.8v.2") }
    val Home: ImageVector by lazy { icon("Home", "M4 11 12 4l8 7v8.5a1.5 1.5 0 0 1-1.5 1.5h-13A1.5 1.5 0 0 1 4 19.5z") }
    val Download: ImageVector by lazy { icon("Download", "M12 4v11M7.5 10.5 12 15l4.5-4.5", "M5 19.5h14") }
    val Paste: ImageVector by lazy { icon("Paste", "M7.5 4.5H16.5A2 2 0 0 1 18.5 6.5V18.5A2 2 0 0 1 16.5 20.5H7.5A2 2 0 0 1 5.5 18.5V6.5A2 2 0 0 1 7.5 4.5Z", "M9 4.5V3h6v1.5M9 10h6M9 14h6") }
    val Send: ImageVector by lazy { icon("Send", "m4 12 16-8-6 16-2.5-6.5z", "m11.5 13.5 3.5-3.5") }
    val Play: ImageVector by lazy { icon("Play", "M3.5 12A8.5 8.5 0 1 1 20.5 12A8.5 8.5 0 1 1 3.5 12Z", solid = "M10.2 8.6v6.8l5.4-3.4z") }
    val Flag: ImageVector by lazy { icon("Flag", "M5.5 21V4M5.5 4.5h11l-2.2 4 2.2 4h-11") }
    val Side: ImageVector by lazy { icon("Side", "M8 3.5H16A2 2 0 0 1 18 5.5V18.5A2 2 0 0 1 16 20.5H8A2 2 0 0 1 6 18.5V5.5A2 2 0 0 1 8 3.5Z", solid = "M12 3.5h4a2 2 0 0 1 2 2v13a2 2 0 0 1-2 2h-4z") }
    val Hand: ImageVector by lazy { icon("Hand", "M1.84 8.963L9.665 7.299L12.16 19.037L4.335 20.701Z", "M14.335 7.299L22.16 8.963L19.665 20.701L11.84 19.037Z", "M9.6 5H14.4A1.6 1.6 0 0 1 16 6.6V15.4A1.6 1.6 0 0 1 14.4 17H9.6A1.6 1.6 0 0 1 8 15.4V6.6A1.6 1.6 0 0 1 9.6 5Z") }
    val Chart: ImageVector by lazy { icon("Chart", "M5 20V12M10 20V6M15 20v-9M20 20V9") }
    val Versions: ImageVector by lazy { icon("Versions", "M3.5 12A8.5 8.5 0 1 1 20.5 12A8.5 8.5 0 1 1 3.5 12Z", "M12 7.5V12l3 2", "M3.5 12H2") }
    val Swords: ImageVector by lazy { icon("Swords", "M14.5 17.5 3 6V3h3l11.5 11.5M13 19l6-6M16 16l4 4M19 21l2-2M9.5 6.5 12 4l3-1h3v3l-1 3-2.5 2.5M5 14l-2 2 3 3 2-2") }
}
