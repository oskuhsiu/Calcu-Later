package me.osku.calcu_later.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput

// 1. State holder class to manage the drawing paths.
class DrawingState {
    // A list of paths that are completed.
    val paths = mutableStateListOf<Path>()

    // The current path being drawn, wrapped in State to trigger recomposition.
    var currentPath by mutableStateOf<Path?>(null)
        private set

    // Starts a new path at the given offset.
    fun startPath(offset: Offset) {
        currentPath = Path().apply { moveTo(offset.x, offset.y) }
    }

    // Appends the next point to the current path.
    // Creates a new Path object from the old one to ensure recomposition.
    fun appendToPath(offset: Offset) {
        currentPath = currentPath?.let {
            Path().apply {
                addPath(it)
                lineTo(offset.x, offset.y)
            }
        }
    }

    // Finalizes the current path by adding it to the list of paths.
    fun endPath() {
        currentPath?.let { paths.add(it) }
        currentPath = null
    }

    // Clears all paths from the canvas.
    fun clear() {
        paths.clear()
        currentPath = null
    }

    // Removes the last drawn path.
    fun undo() {
        if (paths.isNotEmpty()) {
            paths.removeAt(paths.size - 1)
        }
        currentPath = null // Ensure no partial path remains after undo
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
    val stroke = Stroke(width = 5f, cap = StrokeCap.Round, join = StrokeJoin.Round)

    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        drawingState.startPath(offset)
                    },
                    onDragEnd = {
                        drawingState.endPath()
                    },
                    onDragCancel = {
                        drawingState.endPath() // Treat cancel as end of drag
                    }
                ) { change, _ ->
                    drawingState.appendToPath(change.position)
                    change.consume()
                }
            }
    ) {
        // Draw all the completed paths
        drawingState.paths.forEach { path ->
            drawPath(
                path = path,
                color = Color.Black,
                style = stroke
            )
        }
        // Draw the current path being drawn
        drawingState.currentPath?.let {
            drawPath(
                path = it,
                color = Color.Black,
                style = stroke
            )
        }
    }
}
