package me.osku.calcu_later.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
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
    // 已完成的路徑
    val paths = mutableStateListOf<Path>()
    // 使用 mutableStateMapOf 讓 Compose 能即時追蹤變化
    private val activePaths = mutableStateMapOf<Int, Path>()

    // Starts a new path at the given offset.
    fun startPath(pointerId: Int, offset: Offset) {
        activePaths[pointerId] = Path().apply { moveTo(offset.x, offset.y) }
    }

    // Appends the next point to the current path.
    fun appendToPath(pointerId: Int, offset: Offset) {
        activePaths[pointerId]?.let { currentPath ->
            currentPath.lineTo(offset.x, offset.y)
            // 建立新的 Path 物件來強制觸發重組
            val newPath = Path().apply {
                addPath(currentPath)
            }
            activePaths[pointerId] = newPath
        }
    }

    // Finalizes the current path by adding it to the list of paths.
    fun endPath(pointerId: Int) {
        activePaths[pointerId]?.let { paths.add(it) }
        activePaths.remove(pointerId)
    }

    // Clears all paths from the canvas.
    fun clear() {
        paths.clear()
        activePaths.clear()
    }

    // Removes the last drawn path.
    fun undo() {
        if (paths.isNotEmpty()) {
            paths.removeAt(paths.size - 1)
        }
        activePaths.clear()
    }

    // Gets all active paths being drawn.
    fun getActivePaths(): Collection<Path> = activePaths.values
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
        modifier = modifier.pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    // Handle all pointers
                    event.changes.forEach { change ->
                        val id = change.id.value
                        when {
                            change.pressed && !change.previousPressed -> {
                                drawingState.startPath(id.toInt(), change.position)
                            }
                            change.pressed && change.previousPressed -> {
                                drawingState.appendToPath(id.toInt(), change.position)
                            }
                            !change.pressed && change.previousPressed -> {
                                drawingState.endPath(id.toInt())
                            }
                        }
                        change.consume()
                    }
                }
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
        // Draw all active paths being drawn
        drawingState.getActivePaths().forEach { path ->
            drawPath(
                path = path,
                color = Color.Black,
                style = stroke
            )
        }
    }
}
