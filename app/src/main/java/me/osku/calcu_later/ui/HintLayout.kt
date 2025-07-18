package me.osku.calcu_later.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.osku.calcu_later.ArithmeticProblem
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

    // Determine the max width needed for alignment, including the operator
    val maxLength = maxOf(n1Str.length, n2Str.length, ansStr.length)

    // Pad each string with leading spaces to align them to the right
    val num1Padded = n1Str.padStart(maxLength, ' ')
    val num2Padded = n2Str.padStart(maxLength, ' ')
    val ansPadded = ansStr.padStart(maxLength, ' ')
    val carries = calculateCarries(problem).padStart(maxLength, ' ')

    // Use a monospace font for perfect alignment of digits
    val textStyle = MaterialTheme.typography.displayLarge.copy(
        fontFamily = FontFamily.Monospace
    )
    val carryStyle = MaterialTheme.typography.bodyLarge.copy(
        fontFamily = FontFamily.Monospace,
        color = Color.Red.copy(alpha = 0.8f),
        fontSize = 18.sp
    )

    Column(
        horizontalAlignment = Alignment.End,
        modifier = Modifier.padding(end = 32.dp)
    ) {
        // 1. Carry row (only for addition)
        if (carries.isNotBlank()) {
            Text(
                text = carries,
                style = carryStyle,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        // 2. First number
        Text(text = num1Padded, style = textStyle)

        // 3. Operator and second number
        Row {
            Text(text = problem.operator, style = textStyle)
            // Add a spacer for visual separation
            Text(text = " ", style = textStyle)
            Text(text = num2Padded, style = textStyle)
        }

        // 4. Divider
        Divider(color = Color.Black, thickness = 2.dp, modifier = Modifier.padding(top = 8.dp))

        // 5. Answer
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

    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(end = 16.dp)
    ) {
        // 1. First number
        Text(text = n1Str.padStart(totalLength), style = textStyle)

        // 2. Operator and second number
        Row {
            Text(text = problem.operator, style = textStyle)
            Text(text = " ", style = textStyle) // Spacer
            Text(text = n2Str.padStart(totalLength - 2), style = textStyle)
        }

        // 3. First Divider
        Divider(color = Color.Black, thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))

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
        partialSums.forEach {
            Text(text = it, style = textStyle)
        }

        // 5. Second Divider
        Divider(color = Color.Black, thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))

        // 6. Final Answer
        Text(text = ansStr.padStart(totalLength), style = textStyle)
    }
}
