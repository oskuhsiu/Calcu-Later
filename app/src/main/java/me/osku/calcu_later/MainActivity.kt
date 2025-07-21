package me.osku.calcu_later

import android.app.Application
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures // Import for detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput // Import for pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import me.osku.calcu_later.data.SettingsRepository
import me.osku.calcu_later.ui.DrawingCanvas
import me.osku.calcu_later.ui.MultiLayerHintLayout
import me.osku.calcu_later.ui.SettingsScreen
import me.osku.calcu_later.ui.StandardHintLayout
import me.osku.calcu_later.ui.SubtractionHintLayout
import me.osku.calcu_later.ui.rememberDrawingState
import me.osku.calcu_later.ui.theme.CalcuLaterTheme
import kotlin.math.pow
import kotlin.random.Random

// 1. Data class to hold the problem state
data class ArithmeticProblem(
    val number1: Int,
    val number2: Int,
    val operator: String,
    val answer: Int
)

// Enum to manage hint states
enum class HintState {
    None, ShowAnswer, Standard, MultiLayer
}

// 2. ViewModel to manage state and logic
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepository = SettingsRepository(application)

    // Configuration properties
    var digits1 by mutableStateOf(2)
    var digits2 by mutableStateOf(2)
    var allowNegativeResults by mutableStateOf(false)

    // Operation settings
    var operationAddition by mutableStateOf(true)
    var operationSubtraction by mutableStateOf(true)
    var operationMultiplication by mutableStateOf(false)
    var operationDivision by mutableStateOf(false)


    var problem by mutableStateOf(generateProblem())
        private set

    var hintState by mutableStateOf(HintState.None)
        private set

    // State to control settings screen visibility
    var isSettingsScreenVisible by mutableStateOf(false)
        private set

    val showAnswer: Boolean
        get() = hintState == HintState.ShowAnswer || hintState == HintState.Standard || hintState == HintState.MultiLayer

    init {
        // Load initial settings
        digits1 = settingsRepository.digits1
        digits2 = settingsRepository.digits2
        allowNegativeResults = settingsRepository.allowNegativeResults
        operationAddition = settingsRepository.operationAddition
        operationSubtraction = settingsRepository.operationSubtraction
        operationMultiplication = settingsRepository.operationMultiplication
        operationDivision = settingsRepository.operationDivision

        // Observe changes and save them
        snapshotFlow { digits1 }.onEach { settingsRepository.digits1 = it }.launchIn(viewModelScope)
        snapshotFlow { digits2 }.onEach { settingsRepository.digits2 = it }.launchIn(viewModelScope)
        snapshotFlow { allowNegativeResults }.onEach {
            settingsRepository.allowNegativeResults = it
        }.launchIn(viewModelScope)
        snapshotFlow { operationAddition }.onEach { settingsRepository.operationAddition = it }
            .launchIn(viewModelScope)
        snapshotFlow { operationSubtraction }.onEach {
            settingsRepository.operationSubtraction = it
        }.launchIn(viewModelScope)
        snapshotFlow { operationMultiplication }.onEach {
            settingsRepository.operationMultiplication = it
        }.launchIn(viewModelScope)
        snapshotFlow { operationDivision }.onEach { settingsRepository.operationDivision = it }
            .launchIn(viewModelScope)
    }

    fun generateNewProblem() {
        problem = generateProblem()
        hintState = HintState.None
    }

    fun cycleHint() {
        hintState = when (hintState) {
            HintState.None -> HintState.ShowAnswer
            HintState.ShowAnswer -> {
                if (problem.operator == "+") HintState.MultiLayer else HintState.Standard
            }

            HintState.Standard -> HintState.None
            HintState.MultiLayer -> HintState.Standard // Cycle back
        }
    }

    fun showSettings() {
        isSettingsScreenVisible = true
    }

    fun hideSettings() {
        isSettingsScreenVisible = false
    }

    private fun generateProblem(): ArithmeticProblem {
        val safeDigits1 = digits1.coerceAtLeast(1)
        val safeDigits2 = digits2.coerceAtLeast(1)

        var number1 =
            Random.nextInt(10.0.pow(safeDigits1 - 1).toInt(), 10.0.pow(safeDigits1).toInt())
        var number2 =
            Random.nextInt(10.0.pow(safeDigits2 - 1).toInt(), 10.0.pow(safeDigits2).toInt())

        val availableOperators = mutableListOf<String>().apply {
            if (operationAddition) add("+")
            if (operationSubtraction) add("-")
            if (operationMultiplication) add("*")
            if (operationDivision) add("/")
        }

        // Default to addition if no operator is selected
        val operator = if (availableOperators.isNotEmpty()) availableOperators.random() else "+"

        return when (operator) {
            "+" -> ArithmeticProblem(
                number1 = number1,
                number2 = number2,
                operator = "+",
                answer = number1 + number2
            )

            "-" -> {
                if (allowNegativeResults) {
                    ArithmeticProblem(
                        number1 = number1,
                        number2 = number2,
                        operator = "-",
                        answer = number1 - number2
                    )
                } else {
                    val larger = maxOf(number1, number2)
                    val smaller = minOf(number1, number2)
                    ArithmeticProblem(
                        number1 = larger,
                        number2 = smaller,
                        operator = "-",
                        answer = larger - smaller
                    )
                }
            }

            "*" -> {
                // To prevent excessively large numbers in multiplication, cap digits for the problem generation
                val safeMulDigits1 = digits1.coerceAtMost(3)
                val safeMulDigits2 = digits2.coerceAtMost(3)
                val mulNumber1 = Random.nextInt(
                    10.0.pow(safeMulDigits1 - 1).toInt(),
                    10.0.pow(safeMulDigits1).toInt()
                )
                val mulNumber2 = Random.nextInt(
                    10.0.pow(safeMulDigits2 - 1).toInt(),
                    10.0.pow(safeMulDigits2).toInt()
                )
                ArithmeticProblem(
                    number1 = mulNumber1,
                    number2 = mulNumber2,
                    operator = "*",
                    answer = mulNumber1 * mulNumber2
                )
            }

            "/" -> {
                // Ensure divisor is not zero and result is an integer for simplicity
                var divNumber2 = Random.nextInt(1, 10.0.pow(safeDigits2).toInt()) // divisor
                if (divNumber2 == 0) divNumber2 = 1 // Avoid division by zero
                val answer = Random.nextInt(1, 20) // Keep answers in a simple range
                val divNumber1 = divNumber2 * answer
                ArithmeticProblem(
                    number1 = divNumber1,
                    number2 = divNumber2,
                    operator = "/",
                    answer = answer
                )
            }

            else -> throw IllegalStateException("Unknown operator selected")
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setFullScreen()
        setContent {
            CalcuLaterTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: MainViewModel = viewModel()

                    if (viewModel.isSettingsScreenVisible) {
                        SettingsScreen(viewModel = viewModel, onDone = { viewModel.hideSettings() })
                    } else {
                        MainScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        setFullScreen()
    }

    private fun setFullScreen() {
        // 設定全螢幕，隱藏狀態列與導覽列
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val controller = window.insetsController
            controller?.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
            controller?.systemBarsBehavior =
                WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            or View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    )
        }
    }
}

// 4. Composable for the main screen
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val problem = viewModel.problem
    val drawingState = rememberDrawingState()
    val buttonShape = RoundedCornerShape(12.dp)
    val problemTextStyle = MaterialTheme.typography.displayLarge.copy(
        fontSize = MaterialTheme.typography.displayLarge.fontSize * 0.8f
    )


    // When a new problem is generated, clear the canvas
    LaunchedEffect(problem) {
        drawingState.clear()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Problem display
        Column(
            modifier = Modifier
                .fillMaxWidth(0.5f) // Shrink width and height
                .padding(vertical = 4.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(text = "${problem.number1}", style = problemTextStyle)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${problem.operator} ${problem.number2}",
                style = problemTextStyle
            )
            Spacer(modifier = Modifier.height(4.dp))
            Divider(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                thickness = 2.dp
            )

            // Show final answer transparently when hint is active
            if (viewModel.showAnswer) {
                Text(
                    text = "${problem.answer}",
                    style = problemTextStyle.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(
                            alpha = 0.4f
                        )
                    ),
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                // Add a spacer to keep layout consistent when answer is not shown
                Spacer(modifier = Modifier.height(problemTextStyle.fontSize.value.dp + 4.dp))
            }
        }

        // Drawing area with hint overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 16.dp)
                .clip(RoundedCornerShape(16.dp)) // Clip content to rounded shape
                .border(
                    2.dp,
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                    RoundedCornerShape(16.dp)
                )
        ) {
            DrawingCanvas(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                drawingState = drawingState
            )

            // Show hint layouts
            when(viewModel.hintState) {
                HintState.Standard -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (problem.operator == "+") {
                            StandardHintLayout(problem = problem)
                        } else if (problem.operator == "-") {
                            SubtractionHintLayout(problem = problem)
                        }
                    }
                }
                HintState.MultiLayer -> {
                     Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                         if (problem.operator == "+") {
                             MultiLayerHintLayout(problem = problem)
                         }
                    }
                }
                else -> {}
            }
        }

        // Action buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        ) {
            // New Problem Button
            Button(
                onClick = { viewModel.generateNewProblem() },
                shape = buttonShape,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
            ) {
                Text("新題")
            }

            // Undo/Clear Button
            var isLongPress by remember { mutableStateOf(false) }
            var longPressJob: Job? by remember { mutableStateOf<Job?>(null) }
            val coroutineScope = rememberCoroutineScope()
            Button(
                onClick = {
                    if (!isLongPress) drawingState.undo()
                    isLongPress = false
                },
                shape = buttonShape,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                when (event.type) {
                                    PointerEventType.Press -> {
                                        isLongPress = false
                                        longPressJob = coroutineScope.launch {
                                            delay(500L)
                                            isLongPress = true
                                            drawingState.clear()
                                        }
                                    }
                                    PointerEventType.Release -> {
                                        longPressJob?.cancel()
                                    }
                                 }
                            }
                        }
                    }
            ) {
                Text("清除")
            }

            // Hint Button
            Button(
                onClick = { viewModel.cycleHint() },
                shape = buttonShape,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
            ) {
                Text("提示")
            }

            // Settings Button
            Button(
                onClick = { viewModel.showSettings() },
                shape = buttonShape,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
            ) {
                Text("設定")
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    CalcuLaterTheme {
        // Preview requires a plain ViewModel, not AndroidViewModel
        val previewViewModel = object : ViewModel() {
            val problem = ArithmeticProblem(123, 45, "+", 168)
            val showAnswer = true
            val hintState = HintState.None
            fun generateNewProblem() {}
            fun cycleHint() {}
            fun showSettings() {}
        }
        // This is a simplified preview and won't reflect all live functionalities
        MainScreen(viewModel = viewModel())
    }
}
