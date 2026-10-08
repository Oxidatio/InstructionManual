package com.example.instructionmanual

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.instructionmanual.ui.AppRoot
import com.example.instructionmanual.ui.InstructionManualTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            InstructionManualTheme {
                AppRoot()
            }
        }
    }
}
