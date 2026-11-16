package org.example.project

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.tooling.preview.Preview
import kotlin.math.abs

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
@Preview
fun LagrangeApp() {
    val xMin: Double = -5.0
    val xMax: Double = 5.0
    val yMin: Double = -5.0
    val yMax: Double = 5.0
    val sizeInfo = remember{ SizeInfo(xMin, xMax, yMin, yMax) }
    val points = remember { mutableStateListOf<CartesianPoint>() }

    val polynomeText = if (points.size >= 2) "Строится по ${points.size} точкам" else "Недостаточно точек"

    Row {
        Column(modifier = Modifier.weight(10f)) {
            Canvas(modifier = Modifier.fillMaxSize()
                .background(Color.White)
                .onPointerEvent(
                    eventType = PointerEventType.Press
                ) {
                    val point = it.changes.first().position
                    val screenPoint = ScreenPoint(point)
                    val cartesianPoint = screenPoint.toCartesianPoint(sizeInfo)

                    // Защита: проверяем, нет ли уже точки с таким же X (с погрешностью)
                    val existingIndex = points.indexOfFirst { p -> abs(p.x - cartesianPoint.x) < 0.1 }

                    if (existingIndex != -1) {
                        // Если точка с такой абсциссой уже есть — заменяем её Y, чтобы не было деления на ноль
                        points[existingIndex] = cartesianPoint
                    } else {
                        points.add(cartesianPoint)
                    }
                }
            ) {
                sizeInfo.width = this.size.width
                sizeInfo.height = this.size.height
                for (point in points) {
                    val screenPoint = point.toScreenPoint(sizeInfo)
                    drawCircle(
                        color = Color.Red,
                        radius = 5f,
                        center = screenPoint.toOffset()
                    )
                }

                drawLagrangeAxis(this, sizeInfo)
                if (points.size >= 2){
                    drawLagrangeCurve(this, sizeInfo, points)
                }
            }
        }
        Column(modifier = Modifier.weight(3f)) {
            resizeLagrange(sizeInfo)
            Text("P(x): $polynomeText")
        }
    }
}

fun drawLagrangeAxis(drawScope: DrawScope, sizeInfo: SizeInfo) {
    val dp0sp = CartesianPoint(0.0, 0.0).toScreenPoint(sizeInfo)
    drawScope.drawLine(
        color = Color.Black,
        start = Offset(0f, dp0sp.y),
        end = Offset(sizeInfo.width, dp0sp.y),
    )
    drawScope.drawLine(
        color = Color.Black,
        start = Offset(dp0sp.x, 0f),
        end = Offset(dp0sp.x, sizeInfo.height),
    )
}

fun lagrangePolynomial(points: List<CartesianPoint>, x: Double): Double {
    var polynome = 0.0
    for (i in points.indices) {
        var basis = 1.0
        for (j in points.indices) {
            if (i != j) {
                val denominator = points[i].x - points[j].x
                // Защита от деления на ноль, если абсциссы совпали
                if (abs(denominator) < 1e-9) {
                    continue
                }
                basis *= (x - points[j].x) / denominator
            }
        }
        polynome += points[i].y * basis
    }
    return polynome
}

fun drawLagrangeCurve(drawScope: DrawScope, sizeInfo: SizeInfo, points: List<CartesianPoint>) {
    var previousPoint: Offset? = null
    for (i in 0..drawScope.size.width.toInt()){
        val x = ScreenPoint(i.toFloat(), .0f).toCartesianPoint(sizeInfo).x
        val y = lagrangePolynomial(points, x)

        if (y.isNaN() || y.isInfinite()) continue

        val currentPoint = CartesianPoint(x, y).toScreenPoint(sizeInfo).toOffset()
        if (previousPoint != null) {
            drawScope.drawLine(
                color = Color.Blue,
                start = previousPoint,
                end = currentPoint,
            )
        }
        previousPoint = currentPoint
    }
}

@Composable
fun resizeLagrange(sizeInfo: SizeInfo) {
    var lastChangeXorY by remember { mutableStateOf("X") }

    Column {
        Row {
            OutlinedTextField(
                value = sizeInfo.xMin.toString(),
                onValueChange = {
                    sizeInfo.xMin = it.toDoubleOrNull() ?: sizeInfo.xMin
                    lastChangeXorY = "X"
                },
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "<= X <= ",
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = sizeInfo.xMax.toString(),
                onValueChange = {
                    sizeInfo.xMax = it.toDoubleOrNull() ?: sizeInfo.xMax
                    lastChangeXorY = "X"
                },
                modifier = Modifier.weight(1f)
            )
        }
        Row {
            OutlinedTextField(
                value = sizeInfo.yMin.toString(),
                onValueChange = {
                    sizeInfo.yMin = it.toDoubleOrNull() ?: sizeInfo.yMin
                    lastChangeXorY = "Y"
                },
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "<= Y <= ",
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = sizeInfo.yMax.toString(),
                onValueChange = {
                    sizeInfo.yMax = it.toDoubleOrNull() ?: sizeInfo.yMax
                    lastChangeXorY = "Y"
                },
                modifier = Modifier.weight(1f)
            )
        }
        Row {
            Button(onClick = {
                if (lastChangeXorY == "X") {
                    sizeInfo.changeY()
                } else {
                    sizeInfo.changeX()
                }
            }) {
                Text("Масштабировать")
            }
        }
    }
}