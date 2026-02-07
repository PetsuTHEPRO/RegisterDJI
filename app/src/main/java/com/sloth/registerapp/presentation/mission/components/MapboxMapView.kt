package com.sloth.registerapp.presentation.mission.components

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
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.*
import com.mapbox.maps.plugin.gestures.gestures
import com.sloth.registerapp.presentation.mission.model.Waypoint

@Composable
fun MapboxMapView(
    modifier: Modifier = Modifier,
    waypoints: List<Waypoint>,
    primaryColor: Color,
    onMapReady: (MapboxMap) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember { MapView(context) }
    val colorScheme = MaterialTheme.colorScheme

    // Gerenciadores de anotação
    var pointAnnotationManager by remember { mutableStateOf<PointAnnotationManager?>(null) }
    var polylineAnnotationManager by remember { mutableStateOf<PolylineAnnotationManager?>(null) }

    // Efeito para desenhar/atualizar anotações quando a lista de waypoints mudar
    LaunchedEffect(waypoints, pointAnnotationManager, polylineAnnotationManager) {
        pointAnnotationManager?.deleteAll()
        polylineAnnotationManager?.deleteAll()

        waypoints.forEachIndexed { index, waypoint ->
            val pointAnnotationOptions = PointAnnotationOptions()
                .withPoint(Point.fromLngLat(waypoint.longitude, waypoint.latitude))
                // Usa sprite padrão do estilo (evita depender de asset custom inexistente)
                .withIconImage("marker-15")
                .withIconSize(1.3)
                .withTextField((index + 1).toString())
                .withTextColor(colorScheme.onSurface.toArgb())
                .withTextSize(12.0)
            pointAnnotationManager?.create(pointAnnotationOptions)
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
        }
    }

    AndroidView(
        factory = {
            mapView.apply {
                mapboxMap.loadStyle(Style.SATELLITE_STREETS) {
                    // Inicializa os gerenciadores de anotação aqui
                    pointAnnotationManager = annotations.createPointAnnotationManager()
                    polylineAnnotationManager = annotations.createPolylineAnnotationManager()
                    onMapReady(mapboxMap)
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
    zoom: Double = 16.5,
    styleUri: String = Style.LIGHT
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember { MapView(context) }

    var pointAnnotationManager by remember { mutableStateOf<PointAnnotationManager?>(null) }
    var circleAnnotationManager by remember { mutableStateOf<CircleAnnotationManager?>(null) }
    var miniMapboxMap by remember { mutableStateOf<MapboxMap?>(null) }
    val colorScheme = MaterialTheme.colorScheme

    LaunchedEffect(operatorLocation, pointAnnotationManager, circleAnnotationManager, miniMapboxMap) {
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
            .withCircleRadius(6.5)
            .withCircleColor(colorScheme.primary.toArgb())
            .withCircleStrokeColor(colorScheme.onPrimary.toArgb())
            .withCircleStrokeWidth(2.0)
        circleAnnotationManager?.create(circleOptions)

        miniMapboxMap?.setCamera(
            CameraOptions.Builder()
                .center(operatorLocation)
                .zoom(zoom)
                .build()
        )
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
