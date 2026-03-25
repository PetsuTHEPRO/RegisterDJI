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
import com.sloth.registerapp.R
import com.sloth.registerapp.features.mission.data.remote.dto.ServerMissionDto
import java.io.File
import kotlin.math.max

object MissionPreviewStorage {
    private const val PREVIEW_DIR = "mission-previews"
    private const val WIDTH = 420
    private const val HEIGHT = 240

    fun savePreview(context: Context, mission: ServerMissionDto): File? {
        return runCatching {
            val bitmap = createPreviewBitmap(context, mission)
            val file = fileFor(context, mission.id) ?: return null
            try {
                file.outputStream().use { output ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
                }
            } finally {
                bitmap.recycle()
            }
            file
        }.getOrNull()
    }

    fun loadPreview(context: Context, missionId: Int): Bitmap? {
        val file = fileFor(context, missionId) ?: return null
        if (!file.exists()) return null
        return BitmapFactory.decodeFile(file.absolutePath)
    }

    fun renamePreview(context: Context, fromMissionId: Int, toMissionId: Int) {
        if (fromMissionId == toMissionId) return
        val source = fileFor(context, fromMissionId) ?: return
        if (!source.exists()) return
        val target = fileFor(context, toMissionId) ?: return
        source.copyTo(target, overwrite = true)
        source.delete()
    }

    fun deletePreview(context: Context, missionId: Int) {
        fileFor(context, missionId)?.takeIf(File::exists)?.delete()
    }

    private fun fileFor(context: Context, missionId: Int): File? {
        val baseDirectory = context.getExternalFilesDir(null) ?: context.filesDir
        val directory = File(baseDirectory, PREVIEW_DIR)
        if (!directory.exists() && !directory.mkdirs()) {
            return null
        }
        return File(directory, "mission_$missionId.png")
    }

    private fun createPreviewBitmap(context: Context, mission: ServerMissionDto): Bitmap {
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

        val points = mission.waypoints.map { waypoint ->
            waypoint.longitude to waypoint.latitude
        }.ifEmpty {
            listOf(mission.poi_longitude to mission.poi_latitude)
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
}
