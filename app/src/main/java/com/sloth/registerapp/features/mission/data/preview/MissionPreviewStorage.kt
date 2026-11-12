package com.sloth.registerapp.features.mission.data.preview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import com.mapbox.common.MapboxOptions
import com.mapbox.geojson.Point
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapSnapshotOptions
import com.mapbox.maps.Size
import com.mapbox.maps.SnapshotOverlayOptions
import com.mapbox.maps.Snapshotter
import com.mapbox.maps.Style
import com.sloth.registerapp.R
import com.sloth.registerapp.BuildConfig
import com.sloth.registerapp.features.mission.data.mapper.inDisplayOrder
import com.sloth.registerapp.features.mission.data.remote.dto.ServerMissionDto
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.max

object MissionPreviewStorage {
    private const val PREVIEW_DIR = "mission-previews"
    private const val WIDTH = 420
    private const val HEIGHT = 180
    private const val SNAPSHOT_TIMEOUT_MS = 4_000L

    private val generationLocks = java.util.concurrent.ConcurrentHashMap<Int, kotlinx.coroutines.sync.Mutex>()
    private val retryAfter = java.util.concurrent.ConcurrentHashMap<Int, Long>()
    private const val RETRY_DELAY_MS = 5 * 60 * 1000L

    /** A preview is a private, persistent artifact keyed by the mission ID and route signature. */
    suspend fun savePreview(context: Context, mission: ServerMissionDto): File? {
        val file = fileFor(context, mission.id) ?: return null
        val signature = routeSignature(mission)
        if (isValid(file) && signatureFile(file).readTextOrNull() == signature) return file
        if (System.currentTimeMillis() < (retryAfter[mission.id] ?: 0L)) return file.takeIf(::isValid)
        val lock = generationLocks.getOrPut(mission.id) { kotlinx.coroutines.sync.Mutex() }
        return lock.withLock {
            if (isValid(file) && signatureFile(file).readTextOrNull() == signature) return@withLock file
            val bitmap = createPreviewBitmap(context, mission)
            if (bitmap == null || !hasRenderedMapContent(bitmap)) {
                bitmap?.recycle()
                retryAfter[mission.id] = System.currentTimeMillis() + RETRY_DELAY_MS
                return@withLock file.takeIf(::isValid)
            }
            try {
                withContext(Dispatchers.IO) {
                    val temp = File(file.parentFile, "${file.name}.tmp")
                    val written = temp.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    if (!written || !isValid(temp)) {
                        temp.delete()
                        return@withContext
                    }
                    if (!temp.renameTo(file)) {
                        temp.delete()
                        return@withContext
                    }
                    signatureFile(file).writeText(signature)
                }
                retryAfter.remove(mission.id)
            } finally {
                bitmap.recycle()
            }
            file.takeIf(::isValid)
        }
    }

    suspend fun loadPreview(context: Context, missionId: Int): Bitmap? = withContext(Dispatchers.IO) {
        val file = fileFor(context, missionId) ?: return@withContext null
        if (!isValid(file)) return@withContext null
        BitmapFactory.decodeFile(file.absolutePath)
    }

    fun renamePreview(context: Context, fromMissionId: Int, toMissionId: Int) {
        if (fromMissionId == toMissionId) return
        val source = fileFor(context, fromMissionId) ?: return
        val target = fileFor(context, toMissionId) ?: return
        if (!source.exists()) return
        source.renameTo(target)
        signatureFile(source).renameTo(signatureFile(target))
    }

    fun deletePreview(context: Context, missionId: Int) {
        fileFor(context, missionId)?.let { file ->
            file.delete()
            signatureFile(file).delete()
        }
        retryAfter.remove(missionId)
        generationLocks.remove(missionId)
    }

    private fun fileFor(context: Context, missionId: Int): File? {
        val directory = File(context.filesDir, PREVIEW_DIR)
        if (!directory.exists() && !directory.mkdirs()) return null
        val file = File(directory, "mission_$missionId.png")
        // One-time migration from the former app-specific external directory.
        if (!file.exists()) {
            context.getExternalFilesDir(null)?.let { external ->
                val legacy = File(File(external, PREVIEW_DIR), file.name)
                if (legacy.exists()) legacy.copyTo(file, overwrite = false)
            }
        }
        return file
    }

    private fun signatureFile(file: File) = File(file.parentFile, "${file.name}.route")
    private fun File.readTextOrNull(): String? = runCatching { readText() }.getOrNull()
    private fun isValid(file: File): Boolean {
        if (!file.isFile || file.length() < 1024L) return false
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        return bounds.outWidth > 0 && bounds.outHeight > 0
    }

    private fun hasRenderedMapContent(bitmap: Bitmap): Boolean {
        if (bitmap.width < WIDTH || bitmap.height < HEIGHT) return false
        val colors = mutableSetOf<Int>()
        // Sample away from the attribution and most route markers. A blank tile surface
        // stays nearly uniform even when the route overlay itself was rendered.
        for (row in 1..7) for (column in 1..15) {
            val x = bitmap.width * column / 16
            val y = bitmap.height * row / 9
            val pixel = bitmap.getPixel(x, y)
            if (Color.alpha(pixel) < 240) continue
            colors += (Color.red(pixel) / 16 shl 8) or
                (Color.green(pixel) / 16 shl 4) or (Color.blue(pixel) / 16)
        }
        return colors.size >= 12
    }

    private fun routeSignature(mission: ServerMissionDto): String {
        val route = mission.waypoints.inDisplayOrder().joinToString(";") {
            "${it.latitude},${it.longitude},${it.altitude}"
        } + ";${mission.poi_latitude},${mission.poi_longitude}"
        return java.security.MessageDigest.getInstance("SHA-256")
            .digest(("v2:" + route).toByteArray()).joinToString("") { "%02x".format(it) }
    }

    private suspend fun createPreviewBitmap(context: Context, mission: ServerMissionDto): Bitmap? {
        if (BuildConfig.MAPBOX_ACCESS_TOKEN.isBlank()) return null
        return withContext(Dispatchers.Main.immediate) {
            withTimeoutOrNull(SNAPSHOT_TIMEOUT_MS) {
                runCatching { createMapSnapshotBitmapInternal(context, mission) }.getOrNull()
            }
        }
    }

    private suspend fun createMapSnapshotBitmapInternal(context: Context, mission: ServerMissionDto): Bitmap? {
        MapboxOptions.accessToken = BuildConfig.MAPBOX_ACCESS_TOKEN
        val points = mission.snapshotPoints()
        val snapshotter = Snapshotter(
            context,
            MapSnapshotOptions.Builder()
                .size(Size(WIDTH.toFloat(), HEIGHT.toFloat()))
                .pixelRatio(context.resources.displayMetrics.density)
                .build(),
            SnapshotOverlayOptions(showLogo = false, showAttributes = false)
        )
        snapshotter.setStyleUri(Style.SATELLITE_STREETS)
        snapshotter.setCamera(snapshotter.cameraForCoordinates(
            points, EdgeInsets(40.0, 40.0, 40.0, 40.0), null, null
        ))
        return kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { snapshotter.destroy() }
            snapshotter.start(
                overlayCallback = { overlay -> drawMissionOverlay(overlay.canvas, overlay, mission) },
                resultCallback = { snapshot, errorMessage ->
                    if (continuation.isActive) continuation.resume(snapshot)
                    snapshotter.destroy()
                }
            )
        }
    }

    private fun drawMissionOverlay(
        canvas: Canvas,
        overlay: com.mapbox.maps.SnapshotOverlay,
        mission: ServerMissionDto
    ) {
        val orderedWaypoints = mission.waypoints.inDisplayOrder()
        val routePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CC00C2FF")
            style = Paint.Style.STROKE
            strokeWidth = 4f
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
        }
        val poiOuterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        val poiInnerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#00E5A0")
            style = Paint.Style.FILL
        }
        val waypointOuterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF0B1526")
            style = Paint.Style.FILL
        }
        val waypointInnerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF00C2FF")
            style = Paint.Style.FILL
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 18f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        val textHaloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#B3000000")
            textSize = 18f
            textAlign = Paint.Align.CENTER
            style = Paint.Style.STROKE
            strokeWidth = 4f
            isFakeBoldText = true
        }

        val waypointPoints = orderedWaypoints.map {
            Point.fromLngLat(it.longitude, it.latitude)
        }

        if (waypointPoints.size > 1) {
            val path = Path()
            waypointPoints.forEachIndexed { index, point ->
                val screen = overlay.screenCoordinate(point)
                val x = screen.x.toFloat()
                val y = screen.y.toFloat()
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            canvas.drawPath(path, routePaint)
        }

        waypointPoints.forEachIndexed { index, point ->
            val screen = overlay.screenCoordinate(point)
            val x = screen.x.toFloat()
            val y = screen.y.toFloat()
            canvas.drawCircle(x, y, 10f, waypointOuterPaint)
            canvas.drawCircle(x, y, 6.5f, waypointInnerPaint)
            val labelY = y + 5f
            canvas.drawText((index + 1).toString(), x, labelY, textHaloPaint)
            canvas.drawText((index + 1).toString(), x, labelY, textPaint)
        }

        if (mission.poi_latitude != 0.0 || mission.poi_longitude != 0.0) {
            val poiPoint = Point.fromLngLat(mission.poi_longitude, mission.poi_latitude)
            val poiScreen = overlay.screenCoordinate(poiPoint)
            val poiX = poiScreen.x.toFloat()
            val poiY = poiScreen.y.toFloat()
            canvas.drawCircle(poiX, poiY, 8.5f, poiOuterPaint)
            canvas.drawCircle(poiX, poiY, 5.5f, poiInnerPaint)
        }

        drawCompactAttribution(canvas, overlay.attributions)
    }

    private fun drawCompactAttribution(canvas: Canvas, attributions: List<String>) {
        val labels = attributions.mapNotNull { attribution ->
            val lower = attribution.lowercase()
            when {
                "mapbox" in lower -> "Mapbox"
                "openstreetmap" in lower || "osm" in lower -> "OpenStreetMap"
                "maxar" in lower -> "Maxar"
                else -> null
            }
        }.distinct()

        val creditText = if (labels.isNotEmpty()) {
            labels.joinToString(" | ")
        } else {
            "Mapbox | OpenStreetMap | Maxar"
        }

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 9f
        }
        val paddingHorizontal = 8f
        val paddingVertical = 5f
        val textWidth = textPaint.measureText(creditText)
        val boxHeight = textPaint.textSize + paddingVertical * 2
        val left = WIDTH - textWidth - paddingHorizontal * 2 - 10f
        val top = HEIGHT - boxHeight - 8f

        val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#80000000")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(
            RectF(left, top, WIDTH - 10f, HEIGHT - 8f),
            8f,
            8f,
            backgroundPaint
        )
        canvas.drawText(
            creditText,
            left + paddingHorizontal,
            HEIGHT - 8f - paddingVertical,
            textPaint
        )
    }

    private fun createFallbackPreviewBitmap(context: Context, mission: ServerMissionDto): Bitmap {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val background = Paint().apply {
            shader = LinearGradient(
                0f,
                0f,
                WIDTH.toFloat(),
                HEIGHT.toFloat(),
                intArrayOf(
                    Color.parseColor("#061018"),
                    Color.parseColor("#0A1628"),
                    Color.parseColor("#071120")
                ),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), background)

        val gridPaint = Paint().apply {
            color = Color.parseColor("#1400C2FF")
            strokeWidth = 1f
        }
        val step = 32f
        var x = 0f
        while (x <= WIDTH) {
            canvas.drawLine(x, 0f, x, HEIGHT.toFloat(), gridPaint)
            x += step
        }
        var y = 0f
        while (y <= HEIGHT) {
            canvas.drawLine(0f, y, WIDTH.toFloat(), y, gridPaint)
            y += step
        }

        val points = mission.snapshotPoints().map { point ->
            point.longitude() to point.latitude()
        }

        val minX = points.minOf { it.first }
        val maxX = points.maxOf { it.first }
        val minY = points.minOf { it.second }
        val maxY = points.maxOf { it.second }
        val spanX = max(0.0001, maxX - minX)
        val spanY = max(0.0001, maxY - minY)
        val padding = 26f

        val normalized = points.map { (lng, lat) ->
            val px = ((lng - minX) / spanX).toFloat() * (WIDTH - padding * 2) + padding
            val py = ((maxY - lat) / spanY).toFloat() * (HEIGHT - padding * 2) + padding
            px to py
        }

        if (normalized.size > 1) {
            val routePaint = Paint().apply {
                color = Color.parseColor("#8800C2FF")
                style = Paint.Style.STROKE
                strokeWidth = 4f
                isAntiAlias = true
                strokeJoin = Paint.Join.ROUND
                strokeCap = Paint.Cap.ROUND
            }
            val path = Path().apply {
                moveTo(normalized.first().first, normalized.first().second)
                normalized.drop(1).forEach { (px, py) -> lineTo(px, py) }
            }
            canvas.drawPath(path, routePaint)
        }

        val poiPaint = Paint().apply {
            color = Color.parseColor("#00E5A0")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val dotPaint = Paint().apply {
            color = Color.parseColor("#00C2FF")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val ringPaint = Paint().apply {
            color = Color.parseColor("#3300C2FF")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        normalized.forEachIndexed { index, (px, py) ->
            canvas.drawCircle(px, py, if (index == 0) 10f else 7f, ringPaint)
            canvas.drawCircle(px, py, if (index == 0) 5f else 3.5f, dotPaint)
        }

        val poiX = ((mission.poi_longitude - minX) / spanX).toFloat() * (WIDTH - padding * 2) + padding
        val poiY = ((maxY - mission.poi_latitude) / spanY).toFloat() * (HEIGHT - padding * 2) + padding
        canvas.drawCircle(poiX, poiY, 4.5f, poiPaint)

        val tagPaint = Paint().apply {
            color = Color.parseColor("#80000000")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRoundRect(RectF(14f, HEIGHT - 34f, 132f, HEIGHT - 12f), 10f, 10f, tagPaint)
        val textPaint = Paint().apply {
            color = Color.parseColor("#00C2FF")
            textSize = 18f
            isAntiAlias = true
        }
        canvas.drawText(
            context.getString(R.string.mission_preview_route_label),
            24f,
            HEIGHT - 18f,
            textPaint
        )

        return bitmap
    }

    private fun ServerMissionDto.snapshotPoints(): List<Point> {
        val routePoints = waypoints.inDisplayOrder().map { waypoint ->
            Point.fromLngLat(waypoint.longitude, waypoint.latitude)
        }
        val poiPoint = Point.fromLngLat(poi_longitude, poi_latitude)
        val validPoi = poi_latitude != 0.0 || poi_longitude != 0.0
        return (routePoints + if (validPoi) listOf(poiPoint) else emptyList())
            .ifEmpty { listOf(poiPoint) }
    }
}
