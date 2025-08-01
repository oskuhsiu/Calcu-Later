package me.osku.calcu_later.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import me.osku.calcu_later.ArithmeticProblem
import me.osku.calcu_later.ui.theme.ToyOrange
import me.osku.calcu_later.ui.theme.ToyPink
import kotlin.math.pow

/**
 * Calculates the carry digits for an addition problem.
 * Returns a string with carry digits aligned above the numbers.
 * e.g., for 18 + 7, returns "1 "
 * Returns an empty string for subtraction, as borrow logic is more complex.
 */
private fun calculateCarries(problem: ArithmeticProblem): String {
    if (problem.operator != "+") return ""

    val n1Str = problem.number1.toString()
    val n2Str = problem.number2.toString()
    val maxLength = maxOf(n1Str.length, n2Str.length)

    val num1Padded = n1Str.padStart(maxLength, '0')
    val num2Padded = n2Str.padStart(maxLength, '0')

    var carry = 0
    val result = CharArray(maxLength) { ' ' }

    // Iterate from the rightmost digit up to the second digit.
    // The carry from position `i` is placed at `i-1`.
    for (i in maxLength - 1 downTo 1) {
        val sum = num1Padded[i].digitToInt() + num2Padded[i].digitToInt() + carry
        carry = sum / 10
        if (carry > 0) {
            result[i - 1] = '1'
        }
    }

    // Now, calculate the carry from the leftmost digit.
    // This carry will be a new digit at the beginning of the string.
    val sum = num1Padded[0].digitToInt() + num2Padded[0].digitToInt() + carry
    carry = sum / 10

    val finalCarry = if (carry > 0) "1" else ""
    val resultString = String(result)

    val finalResult = finalCarry + resultString

    // If the result is just spaces, it means no carries.
    return if (finalResult.isBlank()) "" else finalResult
}

private data class BorrowInfo(
    val original: Char,
    val afterLending: Char? = null,
    val afterBorrowing: String? = null,
    val lendingColorIndex: Int? = null,
    val borrowingColorIndex: Int? = null
)

private fun calculateBorrowInfo(problem: ArithmeticProblem): Array<BorrowInfo> {
    if (problem.operator != "-") return emptyArray()

    val n1Str = problem.number1.toString()
    val n2Str = problem.number2.toString()
    val maxLength = maxOf(n1Str.length, n2Str.length)
    val num1Padded = n1Str.padStart(maxLength, '0')
    val num2Padded = n2Str.padStart(maxLength, '0')

    val info = num1Padded.map { BorrowInfo(original = it) }.toTypedArray()
    val effectiveDigits = num1Padded.map { it.digitToInt() }.toMutableList()
    var borrowEventCount = 0

    for (i in maxLength - 1 downTo 0) {
        if (effectiveDigits[i] < num2Padded[i].digitToInt()) {
            val colorIndex = borrowEventCount % 5 // 5 is the number of placeColors
            // Position 'i' needs to borrow.
            info[i] = info[i].copy(
                afterBorrowing = (effectiveDigits[i] + 10).toString(),
                borrowingColorIndex = colorIndex
            )
            effectiveDigits[i] += 10

            // Find a lender to the left.
            var j = i - 1
            while (j >= 0) {
                val lenderOriginalEffectiveValue = effectiveDigits[j]
                effectiveDigits[j]--

                info[j] = info[j].copy(
                    afterLending = (lenderOriginalEffectiveValue - 1).digitToChar(),
                    lendingColorIndex = info[j].lendingColorIndex ?: colorIndex
                )

                if (lenderOriginalEffectiveValue > 0) {
                    break // Lender found, stop.
                }
                // Lender was a 0, it becomes 9 and continues the borrow.
                info[j] = info[j].copy(
                    afterLending = '9',
                    lendingColorIndex = info[j].lendingColorIndex ?: colorIndex
                )
                j--
            }
            borrowEventCount++
        }
    }
    return info
}


@Composable
fun StandardHintLayout(problem: ArithmeticProblem) {
    val n1Str = problem.number1.toString()
    val n2Str = problem.number2.toString()
    val ansStr = problem.answer.toString()
    val maxLength = maxOf(n1Str.length, n2Str.length, ansStr.length)

    val num1Padded = n1Str.padStart(maxLength, ' ')
    val num2Padded = n2Str.padStart(maxLength, ' ')
    val ansPadded = ansStr.padStart(maxLength, ' ')
    val carries = calculateCarries(problem).padStart(maxLength, ' ')

    // Use theme colors for place values, making it more vibrant
    val placeColors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        ToyOrange,
        ToyPink
    )

    // 计算每个位置是否需要进位
    val carryPositions = mutableListOf<Boolean>()
    var prevCarry = 0
    for (i in maxLength - 1 downTo 0) {
        val d1 = if (i < num1Padded.length) num1Padded[i].toString().toIntOrNull() ?: 0 else 0
        val d2 = if (i < num2Padded.length) num2Padded[i].toString().toIntOrNull() ?: 0 else 0
        val sum = d1 + d2 + prevCarry
        carryPositions.add(0, sum >= 10)
        prevCarry = sum / 10
    }

    val textStyle = MaterialTheme.typography.displayLarge.copy(
        fontFamily = FontFamily.Monospace
    )

    Column(
        horizontalAlignment = Alignment.End,
        modifier = Modifier.padding(end = 32.dp)
    ) {
        // Carry row
        if (carries.isNotBlank()) {
            Row {
                carries.forEachIndexed { index, digit ->
                    if (digit != ' ') {
                        // 进位符号使用前一个位置的颜色（即右边一位的颜色）
                        // 因为它是由右边位置的数字相加产生的
                        val colorIndex =
                            (maxLength - (index + 1) - 1).coerceAtLeast(0) % placeColors.size
                        Text(
                            text = digit.toString(),
                            style = textStyle.copy(color = placeColors[colorIndex].copy(alpha = 0.7f)),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    } else {
                        Text(text = " ", style = textStyle)
                    }
                }
            }
        }

        // First number
        Row {
            num1Padded.forEachIndexed { index, digit ->
                if (digit != ' ') {
                    val colorIndex = (maxLength - index - 1).coerceAtLeast(0) % placeColors.size
                    if (index < carryPositions.size && carryPositions[index]) {
                        Box(
                            modifier = Modifier
                                .border(
                                    BorderStroke(2.dp, placeColors[colorIndex]),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(4.dp)
                        ) {
                            Text(text = digit.toString(), style = textStyle)
                        }
                    } else {
                        Text(
                            text = digit.toString(),
                            style = textStyle,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                } else {
                    Text(text = " ", style = textStyle)
                }
            }
        }

        // Operator and second number
        Row {
            Text(text = problem.operator, style = textStyle)
            Text(text = " ", style = textStyle)
            num2Padded.forEachIndexed { index, digit ->
                if (digit != ' ') {
                    val colorIndex = (maxLength - index - 1).coerceAtLeast(0) % placeColors.size
                    if (index < carryPositions.size && carryPositions[index]) {
                        Box(
                            modifier = Modifier
                                .border(
                                    BorderStroke(2.dp, placeColors[colorIndex]),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(4.dp)
                        ) {
                            Text(text = digit.toString(), style = textStyle)
                        }
                    } else {
                        Text(
                            text = digit.toString(),
                            style = textStyle,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                } else {
                    Text(text = " ", style = textStyle)
                }
            }
        }

        // Divider
        Divider(
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
            thickness = 2.dp,
            modifier = Modifier.padding(top = 8.dp)
        )

        // Answer
        Text(text = ansPadded, style = textStyle)
    }
}

@Composable
fun SubtractionHintLayout(problem: ArithmeticProblem) {
    if (problem.operator != "-") return

    val n1Str = problem.number1.toString()
    val n2Str = problem.number2.toString()
    val ansStr = problem.answer.toString()
    val maxLength = maxOf(n1Str.length, n2Str.length, ansStr.length)

    val num1Padded = n1Str.padStart(maxLength, ' ')
    val num2Padded = n2Str.padStart(maxLength, ' ')
    val ansPadded = ansStr.padStart(maxLength, ' ')
    val borrowInfo = calculateBorrowInfo(problem)

    val textStyle = MaterialTheme.typography.displayLarge.copy(fontFamily = FontFamily.Monospace)
    val smallTextStyle =
        MaterialTheme.typography.headlineSmall.copy(fontFamily = FontFamily.Monospace)

    val placeColors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        ToyOrange,
        ToyPink
    )

    Column(
        horizontalAlignment = Alignment.End,
        modifier = Modifier.padding(end = 32.dp)
    ) {
        // First number (minuend) with borrow visualization
        Row {
            borrowInfo.forEach { info ->
                if (info != null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Top: Value after it borrowed (e.g., "13")
                        if (info.afterBorrowing != null) {
                            val color =
                                if (info.borrowingColorIndex != null) placeColors[info.borrowingColorIndex] else MaterialTheme.colorScheme.secondary
                            Text(
                                text = info.afterBorrowing,
                                style = smallTextStyle.copy(color = color)
                            )
                        } else {
                            Text(" ", style = smallTextStyle)
                        }

                        // Middle: Value after it lent (e.g., "3"), with strikethrough if it also borrowed
                        if (info.afterLending != null) {
                            val color =
                                if (info.lendingColorIndex != null) placeColors[info.lendingColorIndex] else MaterialTheme.colorScheme.secondary
                            Text(
                                text = info.afterLending.toString(),
                                style = smallTextStyle.copy(
                                    color = color.copy(alpha = 0.7f),
                                    textDecoration = if (info.afterBorrowing != null) TextDecoration.LineThrough else null
                                )
                            )
                        } else {
                            Text(" ", style = smallTextStyle)
                        }


                        // Bottom: Original value, with strikethrough if it changed at all
                        val hasChanged = info.afterLending != null || info.afterBorrowing != null
                        Text(
                            text = info.original.toString(),
                            style = if (hasChanged) {
                                textStyle.copy(
                                    color = textStyle.color.copy(alpha = 0.5f),
                                    textDecoration = TextDecoration.LineThrough
                                )
                            } else {
                                textStyle
                            },
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(" ", style = smallTextStyle)
                        Text(" ", style = smallTextStyle)
                        Text(" ", style = textStyle, modifier = Modifier.padding(horizontal = 4.dp))
                    }
                }
            }
        }

        // Operator and second number
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "${problem.operator} ", style = textStyle)
            num2Padded.forEach { digit ->
                Text(
                    text = digit.toString(),
                    style = textStyle,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }

        // Divider
        Divider(
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
            thickness = 2.dp,
            modifier = Modifier.padding(top = 8.dp)
        )

        // Answer
        Row {
            ansPadded.forEach { digit ->
                Text(
                    text = digit.toString(),
                    style = textStyle,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}


@Composable
fun MultiLayerHintLayout(problem: ArithmeticProblem) {
    // This view is for addition only, for now.
    if (problem.operator != "+") {
        Text(
            "Multi-layer view is currently only available for addition.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(32.dp)
        )
        return
    }

    val n1Str = problem.number1.toString()
    val n2Str = problem.number2.toString()
    val ansStr = problem.answer.toString()
    val maxLength = maxOf(n1Str.length, n2Str.length)
    val totalLength = ansStr.length + 2 // +2 for operator and space

    // Use a monospace font for perfect alignment of digits
    val textStyle = MaterialTheme.typography.headlineMedium.copy(
        fontFamily = FontFamily.Monospace
    )

    // Use theme colors for place values
    val placeColors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        ToyOrange,
        ToyPink
    )

    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(end = 16.dp)
    ) {
        // 1. First number
        Row {
            val n1Parts = n1Str.padStart(totalLength).chunked(1)
            n1Parts.forEachIndexed { index, digit ->
                if (digit.trim().isNotEmpty()) {
                    val colorIndex = (totalLength - index - 1).coerceAtLeast(0) % placeColors.size
                    DigitBox(
                        text = digit,
                        color = placeColors[colorIndex],
                        textStyle = textStyle
                    )
                } else {
                    Text(text = digit, style = textStyle)
                }
            }
        }

        // 2. Operator and second number
        Row {
            Text(text = problem.operator, style = textStyle)
            Text(text = " ", style = textStyle)
            val n2Parts = n2Str.padStart(totalLength - 2).chunked(1)
            n2Parts.forEachIndexed { index, digit ->
                if (digit.trim().isNotEmpty()) {
                    // 计算实际的位数索引（从右往左数）
                    val digitPosition = (n2Parts.size - index - 1)
                    val colorIndex = digitPosition.coerceAtLeast(0) % placeColors.size
                    DigitBox(
                        text = digit,
                        color = placeColors[colorIndex],
                        textStyle = textStyle
                    )
                } else {
                    Text(text = digit, style = textStyle)
                }
            }
        }

        // 3. First Divider
        Divider(
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
            thickness = 1.dp,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        // 4. Partial Sums
        val partialSums = mutableListOf<String>()
        val num1Padded = n1Str.padStart(maxLength, '0')
        val num2Padded = n2Str.padStart(maxLength, '0')

        for (i in 0 until maxLength) {
            val place = maxLength - 1 - i
            val digit1 = num1Padded[place].toString().toInt()
            val digit2 = num2Padded[place].toString().toInt()
            val sum = digit1 + digit2

            if (sum > 0) {
                // Pad the sum with trailing zeros for correct place value, then pad with leading spaces
                val partialSumStr = sum.toString().padEnd(sum.toString().length + i, ' ')
                partialSums.add(partialSumStr.padStart(totalLength))
            }
        }
        partialSums.forEachIndexed { index, sum ->
            Row {
                val sumParts = sum.chunked(1)
                sumParts.forEach { digit ->
                    if (digit.trim().isNotEmpty()) {
                        DigitBox(
                            text = digit,
                            color = placeColors[index],
                            textStyle = textStyle,
                        )
                    } else {
//                        Text(text = digit, style = textStyle)
                        DigitBox(
                            text = digit,
                            color = Color(0x00000000), // Transparent for empty spaces
                            textStyle = textStyle,
                        )
                    }
                }
            }
        }

        // 5. Second Divider
        Divider(
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
            thickness = 1.dp,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        // 6. Final Answer
        Row {
            val ansParts = ansStr.padStart(totalLength).chunked(1)
            ansParts.forEachIndexed { index, digit ->
                if (digit.trim().isNotEmpty()) {
                    val colorIndex = (totalLength - index - 1).coerceAtLeast(0) % placeColors.size
                    DigitBox(
                        text = digit,
                        color = Color(0x00000000), // Transparent for empty spaces
                        //color = placeColors[colorIndex],
                        textStyle = textStyle
                    )
                } else {
                    Text(text = digit, style = textStyle)
                }
            }
        }
    }
}

@Composable
fun AnimatedAdditionHintLayout(problem: ArithmeticProblem) {
    if (problem.operator != "+") return

    val n1Str = problem.number1.toString()
    val n2Str = problem.number2.toString()
    val ansStr = problem.answer.toString()
    val maxLength = maxOf(n1Str.length, n2Str.length)
    val totalLength = ansStr.length + 2 // +2 for operator and space

    // Animation step state: 0 = show problem, 1+ = highlight each place value step by step
    var animationStep by remember { mutableIntStateOf(0) }

    // Use a monospace font for perfect alignment of digits
    val textStyle = MaterialTheme.typography.headlineMedium.copy(
        fontFamily = FontFamily.Monospace
    )

    // Use theme colors for place values
    val placeColors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        ToyOrange,
        ToyPink
    )

    // Calculate partial sums for each place value
    val partialSums = mutableListOf<Pair<String, Int>>() // (sumStr, placeIndex)
    val num1Padded = n1Str.padStart(maxLength, '0')
    val num2Padded = n2Str.padStart(maxLength, '0')

    for (i in 0 until maxLength) {
        val place = maxLength - 1 - i
        val digit1 = num1Padded[place].digitToInt()
        val digit2 = num2Padded[place].digitToInt()
        val sum = digit1 + digit2

        if (sum > 0) {
            // Create the partial sum string with correct place value (add trailing zeros)
            val zerosToAdd = "0".repeat(i)
            val partialSumStr = sum.toString() + zerosToAdd
            partialSums.add(partialSumStr.padStart(totalLength) to i)
        }
    }

    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(end = 16.dp)
    ) {
        // 1. First number with highlighting
        Row {
            val n1Parts = n1Str.padStart(totalLength).chunked(1)
            n1Parts.forEachIndexed { index, digit ->
                if (digit.trim().isNotEmpty()) {
                    val digitPosition = (totalLength - index - 1).coerceAtLeast(0)
                    val colorIndex = digitPosition % placeColors.size
                    val isHighlighted = animationStep > 0 && digitPosition == (animationStep - 1)

                    AnimatedDigitBox(
                        text = digit,
                        color = placeColors[colorIndex],
                        textStyle = textStyle,
                        isHighlighted = isHighlighted
                    )
                } else {
                    Text(text = digit, style = textStyle)
                }
            }
        }

        // 2. Operator and second number with highlighting
        Row {
            Text(text = problem.operator, style = textStyle)
            Text(text = " ", style = textStyle)
            val n2Parts = n2Str.padStart(totalLength - 2).chunked(1)
            n2Parts.forEachIndexed { index, digit ->
                if (digit.trim().isNotEmpty()) {
                    val digitPosition = (n2Parts.size - index - 1).coerceAtLeast(0)
                    val colorIndex = digitPosition % placeColors.size
                    val isHighlighted = animationStep > 0 && digitPosition == (animationStep - 1)

                    AnimatedDigitBox(
                        text = digit,
                        color = placeColors[colorIndex],
                        textStyle = textStyle,
                        isHighlighted = isHighlighted
                    )
                } else {
                    Text(text = digit, style = textStyle)
                }
            }
        }

        // 3. First Divider
        Divider(
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
            thickness = 1.dp,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        // 4. Partial Sums with Animation
        partialSums.forEachIndexed { index, (sum, placeIndex) ->
            AnimatedVisibility(
                visible = animationStep > placeIndex + 1,
                enter = fadeIn(animationSpec = tween(500)) + scaleIn(animationSpec = tween(500))
            ) {
                Row {
                    val sumParts = sum.chunked(1)
                    sumParts.forEach { digit ->
                        if (digit.trim().isNotEmpty()) {
                            DigitBox(
                                text = digit,
                                color = placeColors[placeIndex],
                                textStyle = textStyle
                            )
                        } else {
                            DigitBox(
                                text = digit,
                                color = Color(0x00000000), // Transparent for empty spaces
                                textStyle = textStyle,
                            )
                        }
                    }
                }
            }
        }

        // 5. Second Divider
        AnimatedVisibility(
            visible = animationStep > maxLength + 1,
            enter = fadeIn(animationSpec = tween(300))
        ) {
            Divider(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        // 6. Final Answer
        AnimatedVisibility(
            visible = animationStep > maxLength + 2,
            enter = fadeIn(animationSpec = tween(500)) + scaleIn(animationSpec = tween(500))
        ) {
            Row {
                val ansParts = ansStr.padStart(totalLength).chunked(1)
                ansParts.forEach { digit ->
                    if (digit.trim().isNotEmpty()) {
                        DigitBox(
                            text = digit,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            textStyle = textStyle.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = textStyle.fontSize * 1.1f
                            )
                        )
                    } else {
                        Text(text = digit, style = textStyle)
                    }
                }
            }
        }
    }

    // Animation controller
    LaunchedEffect(problem) {
        animationStep = 0
        delay(800) // Initial delay to show the problem

        // Animate through each place value (from right to left: ones, tens, hundreds, etc.)
        for (i in 1..maxLength) {
            animationStep = i
            delay(1500) // Time to show each step
        }

        // Show divider
        animationStep = maxLength + 1
        delay(500)

        // Show final answer
        animationStep = maxLength + 2
        delay(500)

        // Keep final state
        animationStep = maxLength + 3
    }
}

@Composable
fun AnimatedSubtractionHintLayout(problem: ArithmeticProblem) {
    if (problem.operator != "-") return

    val n1Str = problem.number1.toString()
    val n2Str = problem.number2.toString()
    val ansStr = problem.answer.toString()
    val maxLength = maxOf(n1Str.length, n2Str.length, ansStr.length)

    val num1Padded = n1Str.padStart(maxLength, ' ')
    val num2Padded = n2Str.padStart(maxLength, ' ')
    val ansPadded = ansStr.padStart(maxLength, ' ')
    val borrowInfo = calculateBorrowInfo(problem)

    // Animation step state: 0 = show problem, 1+ = animate each borrow operation step by step
    var animationStep by remember { mutableIntStateOf(0) }

    val textStyle = MaterialTheme.typography.displayLarge.copy(fontFamily = FontFamily.Monospace)
    val smallTextStyle = MaterialTheme.typography.headlineSmall.copy(fontFamily = FontFamily.Monospace)

    val placeColors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        ToyOrange,
        ToyPink
    )

    // Calculate borrow steps - each position that needs borrowing gets a step
    val borrowSteps = mutableListOf<Int>()
    for (i in borrowInfo.indices.reversed()) {
        if (borrowInfo[i]?.afterBorrowing != null) {
            borrowSteps.add(maxLength - 1 - i) // Convert to position from right
        }
    }

    Column(
        horizontalAlignment = Alignment.End,
        modifier = Modifier.padding(end = 32.dp)
    ) {
        // First number (minuend) with animated borrow visualization
        Row {
            borrowInfo.forEachIndexed { index, info ->
                val digitPosition = maxLength - 1 - index
                val isCurrentlyAnimating = animationStep > 0 &&
                    borrowSteps.getOrNull(animationStep - 1) == digitPosition
                val shouldShowBorrowEffect = animationStep > borrowSteps.indexOf(digitPosition) + 1
                val shouldShowLendEffect = info?.lendingColorIndex != null &&
                    animationStep > borrowSteps.indexOfFirst { pos ->
                        val borrowerIndex = maxLength - 1 - pos
                        borrowInfo[borrowerIndex]?.lendingColorIndex == info.lendingColorIndex
                    } + 1

                if (info != null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Top: Value after it borrowed (e.g., "13")
                        AnimatedVisibility(
                            visible = info.afterBorrowing != null && shouldShowBorrowEffect,
                            enter = fadeIn(animationSpec = tween(500)) + scaleIn(animationSpec = tween(500))
                        ) {
                            val color = if (info.borrowingColorIndex != null)
                                placeColors[info.borrowingColorIndex]
                            else MaterialTheme.colorScheme.secondary

                            val animatedColor by animateColorAsState(
                                targetValue = if (isCurrentlyAnimating) color else color.copy(alpha = 0.8f),
                                animationSpec = tween(300)
                            )

                            Text(
                                text = info.afterBorrowing ?: "",
                                style = smallTextStyle.copy(color = animatedColor)
                            )
                        }

                        // Middle: Value after it lent (e.g., "3"), with strikethrough if it also borrowed
                        AnimatedVisibility(
                            visible = info.afterLending != null && shouldShowLendEffect,
                            enter = fadeIn(animationSpec = tween(400))
                        ) {
                            val color = if (info.lendingColorIndex != null)
                                placeColors[info.lendingColorIndex]
                            else MaterialTheme.colorScheme.secondary

                            Text(
                                text = info.afterLending.toString(),
                                style = smallTextStyle.copy(
                                    color = color.copy(alpha = 0.7f),
                                    textDecoration = if (info.afterBorrowing != null)
                                        TextDecoration.LineThrough else null
                                )
                            )
                        }

                        // Bottom: Original value with highlighting and strikethrough animation
                        val hasChanged = info.afterLending != null || info.afterBorrowing != null
                        val shouldHighlight = isCurrentlyAnimating ||
                            (shouldShowBorrowEffect && info.afterBorrowing != null) ||
                            (shouldShowLendEffect && info.afterLending != null)

                        val animatedTextColor by animateColorAsState(
                            targetValue = when {
                                isCurrentlyAnimating -> {
                                    if (info.borrowingColorIndex != null)
                                        placeColors[info.borrowingColorIndex]
                                    else if (info.lendingColorIndex != null)
                                        placeColors[info.lendingColorIndex]
                                    else MaterialTheme.colorScheme.primary
                                }
                                hasChanged && (shouldShowBorrowEffect || shouldShowLendEffect) ->
                                    textStyle.color.copy(alpha = 0.5f)
                                else -> textStyle.color
                            },
                            animationSpec = tween(300)
                        )

                        val animatedBorderWidth by animateFloatAsState(
                            targetValue = if (isCurrentlyAnimating) 3.dp.value else 0.dp.value,
                            animationSpec = tween(300)
                        )

                        Box(
                            modifier = Modifier
                                .then(
                                    if (isCurrentlyAnimating) {
                                        val borderColor = if (info.borrowingColorIndex != null)
                                            placeColors[info.borrowingColorIndex]
                                        else if (info.lendingColorIndex != null)
                                            placeColors[info.lendingColorIndex]
                                        else MaterialTheme.colorScheme.primary

                                        Modifier.border(
                                            animatedBorderWidth.dp,
                                            borderColor,
                                            RoundedCornerShape(4.dp)
                                        ).background(
                                            borderColor.copy(alpha = 0.1f),
                                            RoundedCornerShape(4.dp)
                                        )
                                    } else Modifier
                                )
                                .padding(4.dp)
                        ) {
                            Text(
                                text = info.original.toString(),
                                style = textStyle.copy(
                                    color = animatedTextColor,
                                    textDecoration = if (hasChanged && (shouldShowBorrowEffect || shouldShowLendEffect))
                                        TextDecoration.LineThrough else null
                                ),
                            )
                        }
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(" ", style = smallTextStyle)
                        Text(" ", style = smallTextStyle)
                        Text(" ", style = textStyle, modifier = Modifier.padding(horizontal = 4.dp))
                    }
                }
            }
        }

        // Operator and second number with highlighting
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "${problem.operator} ", style = textStyle)
            num2Padded.forEachIndexed { index, digit ->
                val digitPosition = maxLength - 1 - index
                val isCurrentlyAnimating = animationStep > 0 &&
                    borrowSteps.getOrNull(animationStep - 1) == digitPosition

                if (digit != ' ') {
                    val colorIndex = digitPosition % placeColors.size

                    AnimatedDigitBox(
                        text = digit.toString(),
                        color = placeColors[colorIndex],
                        textStyle = textStyle,
                        isHighlighted = isCurrentlyAnimating
                    )
                } else {
                    Text(
                        text = digit.toString(),
                        style = textStyle,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }

        // Divider
        AnimatedVisibility(
            visible = animationStep > borrowSteps.size + 1,
            enter = fadeIn(animationSpec = tween(300))
        ) {
            Divider(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                thickness = 2.dp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Answer
        AnimatedVisibility(
            visible = animationStep > borrowSteps.size + 2,
            enter = fadeIn(animationSpec = tween(500)) + scaleIn(animationSpec = tween(500))
        ) {
            Row {
                ansPadded.forEach { digit ->
                    Text(
                        text = digit.toString(),
                        style = textStyle.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = textStyle.fontSize * 1.05f
                        ),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }
    }

    // Animation controller
    LaunchedEffect(problem) {
        animationStep = 0
        delay(800) // Initial delay to show the problem

        // Animate through each borrow operation (from right to left)
        borrowSteps.forEachIndexed { index, _ ->
            animationStep = index + 1
            delay(2000) // Time to show each borrow step
        }

        // Show divider
        animationStep = borrowSteps.size + 1
        delay(500)

        // Show final answer
        animationStep = borrowSteps.size + 2
        delay(500)

        // Keep final state
        animationStep = borrowSteps.size + 3
    }
}

@Composable
fun AnimatedDigitBox(
    text: String,
    color: Color,
    textStyle: TextStyle,
    isHighlighted: Boolean,
    modifier: Modifier = Modifier
) {
    val animatedBorderWidth by animateFloatAsState(
        targetValue = if (isHighlighted) 4.dp.value else 2.dp.value,
        animationSpec = tween(300)
    )

    val animatedColor by animateColorAsState(
        targetValue = if (isHighlighted) color else color.copy(alpha = 0.7f),
        animationSpec = tween(300)
    )

    val animatedBackgroundColor by animateColorAsState(
        targetValue = if (isHighlighted) color.copy(alpha = 0.1f) else Color.Transparent,
        animationSpec = tween(300)
    )

    Box(
        modifier = modifier
            .border(animatedBorderWidth.dp, animatedColor, RoundedCornerShape(4.dp))
            .background(animatedBackgroundColor, RoundedCornerShape(4.dp))
            .padding(4.dp)
    ) {
        Text(
            text = text,
            style = textStyle.copy(
                color = if (isHighlighted) color else textStyle.color
            )
        )
    }
}

@Composable
fun DigitBox(
    text: String,
    color: Color,
    textStyle: TextStyle,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .border(2.dp, color, RoundedCornerShape(4.dp))
            .padding(4.dp)
    ) {
        Text(text = text, style = textStyle)
    }
}
