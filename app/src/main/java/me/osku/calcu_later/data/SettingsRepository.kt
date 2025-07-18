package me.osku.calcu_later.data

import android.content.Context
import android.content.SharedPreferences

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_DIGITS_1 = "digits1"
        private const val KEY_DIGITS_2 = "digits2"
        private const val KEY_ALLOW_NEGATIVE_RESULTS = "allow_negative_results"
        private const val KEY_OP_ADDITION = "op_addition"
        private const val KEY_OP_SUBTRACTION = "op_subtraction"
        private const val KEY_OP_MULTIPLICATION = "op_multiplication"
        private const val KEY_OP_DIVISION = "op_division"
    }

    var digits1: Int
        get() = prefs.getInt(KEY_DIGITS_1, 2)
        set(value) = prefs.edit().putInt(KEY_DIGITS_1, value).apply()

    var digits2: Int
        get() = prefs.getInt(KEY_DIGITS_2, 2)
        set(value) = prefs.edit().putInt(KEY_DIGITS_2, value).apply()

    var allowNegativeResults: Boolean
        get() = prefs.getBoolean(KEY_ALLOW_NEGATIVE_RESULTS, false)
        set(value) = prefs.edit().putBoolean(KEY_ALLOW_NEGATIVE_RESULTS, value).apply()
        
    var operationAddition: Boolean
        get() = prefs.getBoolean(KEY_OP_ADDITION, true)
        set(value) = prefs.edit().putBoolean(KEY_OP_ADDITION, value).apply()

    var operationSubtraction: Boolean
        get() = prefs.getBoolean(KEY_OP_SUBTRACTION, true)
        set(value) = prefs.edit().putBoolean(KEY_OP_SUBTRACTION, value).apply()

    var operationMultiplication: Boolean
        get() = prefs.getBoolean(KEY_OP_MULTIPLICATION, false)
        set(value) = prefs.edit().putBoolean(KEY_OP_MULTIPLICATION, value).apply()

    var operationDivision: Boolean
        get() = prefs.getBoolean(KEY_OP_DIVISION, false)
        set(value) = prefs.edit().putBoolean(KEY_OP_DIVISION, value).apply()
}
