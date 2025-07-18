package me.osku.calcu_later.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.osku.calcu_later.MainViewModel

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onDone: () -> Unit
) {
    var tempDigits1 by remember { mutableStateOf(viewModel.digits1.toString()) }
    var tempDigits2 by remember { mutableStateOf(viewModel.digits2.toString()) }

    val onDoneClick = {
        viewModel.digits1 = tempDigits1.toIntOrNull() ?: viewModel.digits1
        viewModel.digits2 = tempDigits2.toIntOrNull() ?: viewModel.digits2
        onDone()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("設定", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(32.dp))

        // --- Number of Digits ---
        SectionTitle("數字位數")
        DigitsSettingRow("數字1 位數:", tempDigits1) { tempDigits1 = it }
        DigitsSettingRow("數字2 位數:", tempDigits2) { tempDigits2 = it }

        Spacer(modifier = Modifier.height(16.dp))
        Divider()
        Spacer(modifier = Modifier.height(16.dp))

        // --- Allowed Operations ---
        SectionTitle("運算類型")
        OperationCheckbox("加法 (+)", viewModel.operationAddition) { viewModel.operationAddition = it }
        OperationCheckbox("減法 (-)", viewModel.operationSubtraction) { viewModel.operationSubtraction = it }
        OperationCheckbox("乘法 (×)", viewModel.operationMultiplication) { viewModel.operationMultiplication = it }
        OperationCheckbox("除法 (÷)", viewModel.operationDivision) { viewModel.operationDivision = it }

        if (viewModel.operationSubtraction) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("允許負數結果")
                Switch(
                    checked = viewModel.allowNegativeResults,
                    onCheckedChange = { viewModel.allowNegativeResults = it }
                )
            }
        }


        Spacer(modifier = Modifier.weight(1f)) // Push button to the bottom

        Button(
            onClick = onDoneClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("完成")
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    )
}

@Composable
private fun DigitsSettingRow(label: String, value: String, onValueChange: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = { onValueChange(((value.toIntOrNull() ?: 1) - 1).coerceAtLeast(1).toString()) },
                modifier = Modifier.padding(end = 8.dp)
            ) { Text("-") }
            Text(value, modifier = Modifier.padding(horizontal = 8.dp))
            Button(
                onClick = { onValueChange(((value.toIntOrNull() ?: 1) + 1).toString()) },
                modifier = Modifier.padding(start = 8.dp)
            ) { Text("+") }
        }
    }
}

@Composable
private fun OperationCheckbox(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(text = label, modifier = Modifier.padding(start = 8.dp))
    }
}
