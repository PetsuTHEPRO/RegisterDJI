package com.sloth.registerapp.presentation.mission.components

import android.location.Location
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapInitOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.dsl.cameraOptions
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.*
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import com.mapbox.maps.plugin.gestures.gestures
import com.sloth.registerapp.presentation.mission.model.Waypoint

@Composable
fun MapboxMapView(
    modifier: Modifier = Modifier,
    waypoints: List<Waypoint>,
    pointOfInterest: Point? = null,
    pendingPoint: Point? = null,
    selectedWaypointIndex: Int? = null,
    primaryColor: Color,
    onMapClick: ((Point) -> Boolean)? = null,
    onMapReady: (MapboxMap) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember {
        MapView(
            context,
            MapInitOptions(
                context = context,
                textureView = true
            )
        )
    }
    val colorScheme = MaterialTheme.colorScheme
    val latestMapClick by rememberUpdatedState(onMapClick)

    // Gerenciadores de anotação
    var pointAnnotationManager by remember { mutableStateOf<PointAnnotationManager?>(null) }
    var circleAnnotationManager by remember { mutableStateOf<CircleAnnotationManager?>(null) }
    var polylineAnnotationManager by remember { mutableStateOf<PolylineAnnotationManager?>(null) }

    // Efeito para desenhar/atualizar anotações quando a lista de waypoints mudar
    LaunchedEffect(
        waypoints,
        pointOfInterest,
        pendingPoint,
        selectedWaypointIndex,
        pointAnnotationManager,
        circleAnnotationManager,
        polylineAnnotationManager
    ) {
        pointAnnotationManager?.deleteAll()
        circleAnnotationManager?.deleteAll()
        polylineAnnotationManager?.deleteAll()

        pointOfInterest?.let { poi ->
            circleAnnotationManager?.create(
                CircleAnnotationOptions()
                    .withPoint(poi)
                    .withCircleRadius(5.5)
                    .withCircleColor(Color(0xFFFF4D4F).toArgb())
                    .withCircleStrokeColor(Color.White.toArgb())
                    .withCircleStrokeWidth(1.6)
            )
        }

        pendingPoint?.let { point ->
            circleAnnotationManager?.create(
                CircleAnnotationOptions()
                    .withPoint(point)
                    .withCircleRadius(6.0)
                    .withCircleColor(colorScheme.tertiary.toArgb())
                    .withCircleStrokeColor(Color.White.toArgb())
                    .withCircleStrokeWidth(1.4)
            )
            pointAnnotationManager?.create(
                PointAnnotationOptions()
                    .withPoint(point)
                    .withIconImage("marker-15")
                    .withIconOpacity(0.0)
                    .withTextField("+")
                    .withTextColor(Color.White.toArgb())
                    .withTextSize(12.0)
            )
        }

        waypoints.forEachIndexed { index, waypoint ->
            val isSelected = selectedWaypointIndex == index
            val waypointPoint = Point.fromLngLat(waypoint.longitude, waypoint.latitude)

            circleAnnotationManager?.create(
                CircleAnnotationOptions()
                    .withPoint(waypointPoint)
                    .withCircleRadius(if (isSelected) 10.0 else 8.6)
                    .withCircleColor(Color(0xFF1E88FF).toArgb())
                    .withCircleStrokeColor((if (isSelected) Color.Black else Color(0xFF455A64)).toArgb())
                    .withCircleStrokeWidth(if (isSelected) 2.4 else 2.0)
            )

            pointAnnotationManager?.create(
                PointAnnotationOptions()
                    .withPoint(waypointPoint)
                    .withIconImage("marker-15")
                    .withIconOpacity(0.0)
                    .withTextField((index + 1).toString())
                    .withTextColor(Color.White.toArgb())
                    .withTextHaloColor(Color.Black.toArgb())
                    .withTextHaloWidth(1.4)
                    .withTextSize(if (isSelected) 13.5 else 12.5)
            )
        }

        if (waypoints.size > 1) {
            val points = waypoints.map { Point.fromLngLat(it.longitude, it.latitude) }
            val polylineAnnotationOptions = PolylineAnnotationOptions()
                .withPoints(points)
                .withLineColor(primaryColor.toArgb())
                .withLineWidth(3.0)
            polylineAnnotationManager?.create(polylineAnnotationOptions)
        }
    }

    // Gerencia o ciclo de vida da MapView
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            pointAnnotationManager?.deleteAll()
            circleAnnotationManager?.deleteAll()
            polylineAnnotationManager?.deleteAll()
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    AndroidView(
        factory = {
            mapView.apply {
                mapboxMap.loadStyle(Style.SATELLITE_STREETS) {
                    // Inicializa os gerenciadores de anotação aqui
                    pointAnnotationManager = annotations.createPointAnnotationManager()
                    circleAnnotationManager = annotations.createCircleAnnotationManager()
                    polylineAnnotationManager = annotations.createPolylineAnnotationManager()
                    onMapReady(mapboxMap)
                }
                gestures.addOnMapClickListener { point ->
                    latestMapClick?.invoke(point) ?: false
                }
            }
        },
        modifier = modifier
    )
}

@Composable
fun MapboxMiniMapView(
    modifier: Modifier = Modifier,
    operatorLocation: Point,
    operatorAccuracyMeters: Float? = null,
    operatorIsStationary: Boolean = false,
    zoom: Double = 16.5,
    styleUri: String = Style.LIGHT
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember {
        MapView(
            context,
            MapInitOptions(
                context = context,
                textureView = true
            )
        )
    }

    var pointAnnotationManager by remember { mutableStateOf<PointAnnotationManager?>(null) }
    var circleAnnotationManager by remember { mutableStateOf<CircleAnnotationManager?>(null) }
    var miniMapboxMap by remember { mutableStateOf<MapboxMap?>(null) }
    var lastCameraCenter by remember { mutableStateOf<Point?>(null) }
    val colorScheme = MaterialTheme.colorScheme

    LaunchedEffect(
        operatorLocation,
        operatorAccuracyMeters,
        operatorIsStationary,
        pointAnnotationManager,
        circleAnnotationManager,
        miniMapboxMap
    ) {
        pointAnnotationManager?.deleteAll()
        circleAnnotationManager?.deleteAll()

        val pointAnnotationOptions = PointAnnotationOptions()
            .withPoint(operatorLocation)
            .withIconImage("marker-15")
            .withIconSize(1.3)
            .withTextField("OP")
            .withTextSize(10.0)
            .withTextColor(colorScheme.onSurface.toArgb())

        pointAnnotationManager?.create(pointAnnotationOptions)

        val circleOptions = CircleAnnotationOptions()
            .withPoint(operatorLocation)
            .withCircleRadius(
                ((operatorAccuracyMeters ?: 8f) / 4f)
                    .coerceIn(if (operatorIsStationary) 7.5f else 6.5f, 12f)
                    .toDouble()
            )
            .withCircleColor(colorScheme.primary.toArgb())
            .withCircleStrokeColor(colorScheme.onPrimary.toArgb())
            .withCircleStrokeWidth(2.0)
        circleAnnotationManager?.create(circleOptions)

        miniMapboxMap?.let { map ->
            val shouldCenterCamera = lastCameraCenter == null ||
                distanceMeters(lastCameraCenter, operatorLocation) >= MINI_MAP_CAMERA_RECENTER_THRESHOLD_METERS

            if (shouldCenterCamera) {
                map.setCamera(
                    CameraOptions.Builder()
                        .center(operatorLocation)
                        .zoom(zoom)
                        .build()
                )
                lastCameraCenter = operatorLocation
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            pointAnnotationManager?.deleteAll()
            circleAnnotationManager?.deleteAll()
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    AndroidView(
        factory = {
            mapView.apply {
                this.mapboxMap.loadStyle(styleUri) {
                    pointAnnotationManager = annotations.createPointAnnotationManager()
                    circleAnnotationManager = annotations.createCircleAnnotationManager()
                    miniMapboxMap = this.mapboxMap

                    gestures.rotateEnabled = false
                    gestures.pitchEnabled = false
                    gestures.scrollEnabled = false
                    gestures.doubleTapToZoomInEnabled = false
                    gestures.quickZoomEnabled = false
                }
            }
        },
        modifier = modifier
    )
}

private const val MINI_MAP_CAMERA_RECENTER_THRESHOLD_METERS = 12.0

private fun distanceMeters(from: Point?, to: Point): Double {
    if (from == null) return Double.MAX_VALUE
    val result = FloatArray(1)
    Location.distanceBetween(
        from.latitude(),
        from.longitude(),
        to.latitude(),
        to.longitude(),
        result
    )
    return result[0].toDouble()
}

@Composable
fun MissionPreviewMapView(
    modifier: Modifier = Modifier,
    previewPoints: List<Point>,
    pointOfInterest: Point?,
    primaryColor: Color,
    styleUri: String = Style.SATELLITE_STREETS
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember {
        MapView(
            context,
            MapInitOptions(
                context = context,
                textureView = true
            )
        )
    }
    val colorScheme = MaterialTheme.colorScheme

    var pointAnnotationManager by remember { mutableStateOf<PointAnnotationManager?>(null) }
    var polylineAnnotationManager by remember { mutableStateOf<PolylineAnnotationManager?>(null) }
    var mapboxMap by remember { mutableStateOf<MapboxMap?>(null) }

    LaunchedEffect(previewPoints, pointOfInterest, pointAnnotationManager, polylineAnnotationManager, mapboxMap) {
        pointAnnotationManager?.deleteAll()
        polylineAnnotationManager?.deleteAll()

        val pointsToFrame = buildList {
            addAll(previewPoints)
            pointOfInterest?.let(::add)
        }

        if (previewPoints.size > 1) {
            polylineAnnotationManager?.create(
                PolylineAnnotationOptions()
                    .withPoints(previewPoints)
                    .withLineColor(primaryColor.toArgb())
                    .withLineWidth(3.5)
            )
        }

        previewPoints.forEachIndexed { index, point ->
            pointAnnotationManager?.create(
                PointAnnotationOptions()
                    .withPoint(point)
                    .withIconImage("marker-15")
                    .withIconSize(if (index == 0) 1.25 else 1.1)
            )
        }

        pointOfInterest?.let { poi ->
            pointAnnotationManager?.create(
                PointAnnotationOptions()
                    .withPoint(poi)
                    .withIconImage("marker-15")
                    .withIconSize(1.35)
                    .withTextField("POI")
                    .withTextColor(colorScheme.primary.toArgb())
                    .withTextSize(10.0)
            )
        }

        mapboxMap?.let { map ->
            if (pointsToFrame.isNotEmpty()) {
                map.cameraForCoordinates(
                    pointsToFrame,
                    camera = cameraOptions { },
                    coordinatesPadding = EdgeInsets(42.0, 42.0, 42.0, 42.0),
                    maxZoom = 16.5,
                    offset = null
                ) { fittedCamera ->
                    map.setCamera(fittedCamera)
                }
            } else {
                map.setCamera(
                    CameraOptions.Builder()
                        .center(Point.fromLngLat(0.0, 0.0))
                        .zoom(14.5)
                        .build()
                )
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            pointAnnotationManager?.deleteAll()
            polylineAnnotationManager?.deleteAll()
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    AndroidView(
        factory = {
            mapView.apply {
                gestures.rotateEnabled = false
                gestures.pitchEnabled = false
                gestures.scrollEnabled = false
                gestures.doubleTapToZoomInEnabled = false
                gestures.quickZoomEnabled = false
                gestures.pinchToZoomEnabled = false

                this.mapboxMap.loadStyle(styleUri) {
                    pointAnnotationManager = annotations.createPointAnnotationManager()
                    polylineAnnotationManager = annotations.createPolylineAnnotationManager()
                    mapboxMap = this.mapboxMap
                }
            }
        },
        modifier = modifier
    )
}
