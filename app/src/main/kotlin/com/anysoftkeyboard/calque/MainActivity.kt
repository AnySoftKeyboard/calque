package com.anysoftkeyboard.calque

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.anysoftkeyboard.calque.ui.CalqueApp
import com.anysoftkeyboard.calque.ui.theme.CalqueTheme

/** Main activity hosting the primary Compose navigation shell of Calque. */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as? CalqueApplication
        val appContainer = app?.appContainer ?: return

        setContent { CalqueTheme { CalqueApp(appContainer = appContainer) } }
    }
}
