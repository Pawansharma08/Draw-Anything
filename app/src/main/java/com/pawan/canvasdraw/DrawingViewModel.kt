package com.pawan.canvasdraw

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class DrawingState(
    val selectedColor: Color = Color.Black,
    val currPath: PathData? = null,
    val paths: List<PathData> = emptyList(),
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val redoPaths: List<PathData> = emptyList() // Tracks redo paths
)

val allColors = listOf(
    Color.Black,
    Color.Blue,
    Color.Red,
    Color.Green,
    Color.Yellow,
    Color.Magenta,
    Color.Cyan
)

data class PathData(
    val id: String,
    val color: Color,
    val path: List<Offset>
)

sealed interface DrawingAction {
    data object onNewPathStart : DrawingAction
    data class onDraw(val offset: Offset) : DrawingAction
    data object onPathEnd : DrawingAction
    data class onSelectColor(val color: Color) : DrawingAction
    data object onClearCanvasClick : DrawingAction
    data object onUndoClick : DrawingAction
    data object onRedoClick : DrawingAction
}

class DrawingViewModel : ViewModel() {

    private val _state = MutableStateFlow(DrawingState())
    val state = _state.asStateFlow()

    // Stacks for undo and redo actions
    private val undoStack = mutableListOf<PathData>()
    private val redoStack = mutableListOf<PathData>()

    fun onAction(action: DrawingAction) {
        when (action) {
            DrawingAction.onClearCanvasClick -> onClearCanvasClick()
            is DrawingAction.onDraw -> onDraw(action.offset)
            DrawingAction.onNewPathStart -> onNewPathStart()
            DrawingAction.onPathEnd -> onPathEnd()
            is DrawingAction.onSelectColor -> onSelectColor(action.color)
            DrawingAction.onUndoClick -> onUndoClick()
            DrawingAction.onRedoClick -> onRedoClick()
        }
    }

    private fun onSelectColor(color: Color) {
        _state.update {
            it.copy(
                selectedColor = color
            )
        }
    }

    private fun onPathEnd() {
        val currPathData = state.value.currPath ?: return

        _state.update {
            it.copy(
                currPath = null,
                paths = it.paths + currPathData
            )
        }

        // Add to undo stack and clear redo stack
        undoStack.add(currPathData)
        redoStack.clear()

        updateUndoRedoState()
    }

    private fun onNewPathStart() {
        _state.update {
            it.copy(
                currPath = PathData(
                    id = System.currentTimeMillis().toString(),
                    color = it.selectedColor,
                    path = emptyList()
                )
            )
        }
    }

    private fun onClearCanvasClick() {
        undoStack.clear()
        redoStack.clear()

        _state.update {
            it.copy(
                currPath = null,
                paths = emptyList(),
                canUndo = false,
                canRedo = false
            )
        }
    }

    private fun onDraw(offset: Offset) {
        val currPathData = state.value.currPath ?: return

        _state.update {
            it.copy(
                currPath = currPathData.copy(
                    path = currPathData.path + offset
                )
            )
        }
    }

    private fun onUndoClick() {
        if (undoStack.isNotEmpty()) {
            val lastPath = undoStack.removeAt(undoStack.lastIndex)
            redoStack.add(lastPath)

            _state.update {
                it.copy(
                    paths = undoStack.toList()
                )
            }

            updateUndoRedoState()
        }
    }

    private fun onRedoClick() {
        if (redoStack.isNotEmpty()) {
            val lastPath = redoStack.removeLast()
            undoStack.add(lastPath)

            _state.update {
                it.copy(
                    paths = undoStack.toList()
                )
            }

            updateUndoRedoState()
        }
    }

    private fun updateUndoRedoState() {
        _state.update {
            it.copy(
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty()
            )
        }
    }
}
