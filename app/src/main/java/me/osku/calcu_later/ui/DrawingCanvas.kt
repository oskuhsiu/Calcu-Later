package me.osku.calcu_later.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput

// 1. State holder class to manage the drawing paths using a list of points.
class DrawingState {
    // A list of paths, where each path is a list of points (Offsets).
    private val _paths = mutableStateListOf<List<Offset>>()
    val paths: List<List<Offset>> = _paths

    // Starts a new path at the given offset.
    fun startPath(offset: Offset) {
        _paths.add(listOf(offset))
    }

    // Appends the next point to the current path.
    fun appendToPath(offset: Offset) {
        if (_paths.isNotEmpty()) {
            val lastPath = _paths.last()
            // Create a new list with the new point added.
            _paths[_paths.lastIndex] = lastPath + offset
        }
    }

    // Clears all paths from the canvas.
    fun clear() {
        _paths.clear()
    }
}

// 2. A Composable function to remember the DrawingState across recompositions.
@Composable
fun rememberDrawingState(): DrawingState {
    return remember { DrawingState() }
}

// 3. The main DrawingCanvas Composable.
@Composable
fun DrawingCanvas(
    modifier: Modifier = Modifier,
    drawingState: DrawingState
) {
    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                forEachGesture {
                    awaitPointerEventScope {
                        // Wait for a press event, without consuming it.
                        // This allows other gestures to be detected, if needed.
                        val down = awaitFirstDown(requireUnconsumed = false)
                        drawingState.startPath(down.position)

                        // Loop to track pointer movements
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull()

                            if (change != null) {
                                // If pointer is up, break the loop
                                if (!change.pressed) {
                                    break
                                }
                                // While pressed and moving, add to path
                                drawingState.appendToPath(change.position)
                                // Consume the change to indicate it's been handled
                                change.consume()
                            } else {
                                // No change, break the loop
                                break
                            }
                        }
                    }
                }
            }
    ) {
        // When the canvas is recomposed, it will redraw all the paths.
        drawingState.paths.forEach { pathPoints ->
            if (pathPoints.isEmpty()) return@forEach

            val path = Path().apply {
                moveTo(pathPoints.first().x, pathPoints.first().y)
                pathPoints.forEach { point ->
                    lineTo(point.x, point.y)
                }
            }
            drawPath(
                path = path,
                color = Color.Black,
                style = Stroke(
                    width = 5f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}
