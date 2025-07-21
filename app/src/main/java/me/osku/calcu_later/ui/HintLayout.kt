package me.osku.calcu_later.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                        val colorIndex = (maxLength - (index + 1) - 1).coerceAtLeast(0) % placeColors.size
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
