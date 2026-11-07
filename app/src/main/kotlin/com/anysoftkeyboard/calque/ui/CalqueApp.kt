package com.anysoftkeyboard.calque.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.anysoftkeyboard.calque.R
import com.anysoftkeyboard.calque.di.AppContainer
import com.anysoftkeyboard.calque.ui.nav.NavDestination
import com.anysoftkeyboard.calque.ui.reader.ReaderScreen
import com.anysoftkeyboard.calque.ui.reader.ReaderViewModel
import com.anysoftkeyboard.calque.ui.theme.CalqueTheme

/** Primary top-level navigation shell for Calque. */
@Composable
fun CalqueApp(
    appContainer: AppContainer,
    modifier: Modifier = Modifier,
) {
    val navManager = appContainer.navStateManager
    var currentDestination by remember { mutableStateOf(navManager.currentDestination) }
    // Owned by the ViewModelStore so reader state survives configuration changes and its scope
    // is cancelled in onCleared().
    val readerViewModel: ReaderViewModel =
        viewModel(factory = viewModelFactory { initializer { ReaderViewModel(appContainer) } })
    val colors = CalqueTheme.colors

    DisposableEffect(navManager) {
        val listener: (NavDestination) -> Unit = { dest -> currentDestination = dest }
        navManager.addListener(listener)
        currentDestination = navManager.currentDestination
        onDispose { navManager.removeListener(listener) }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.background,
        topBar = { HeaderBar(destination = currentDestination) },
        bottomBar = {
            BottomNavBar(
                currentDestination = currentDestination,
                onSelectDestination = { navManager.navigateTo(it) },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
            when (currentDestination) {
                NavDestination.READER -> {
                    ReaderScreen(viewModel = readerViewModel)
                }
                NavDestination.TOOLBOX -> {
                    PlaceholderScreen(
                        content =
                            "Toolbox Toys:\n\n" +
                                "• False Friend Radar\n" +
                                "• Word Transformer\n" +
                                "• Tense Shifter\n" +
                                "• Number Odometer\n" +
                                "• Clitic Surgery",
                    )
                }
                NavDestination.VAULT -> {
                    PlaceholderScreen(
                        content = "Vault:\n\nSaved words and custom texts will appear here.",
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderBar(destination: NavDestination) {
    val colors = CalqueTheme.colors
    val appName = stringResource(R.string.app_name)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colors.surface,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
    ) {
        Column(
            modifier =
                Modifier.fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Text(
                text = "$appName — ${destination.title}",
                fontSize = 22.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = destination.subtitle,
                fontSize = 13.sp,
                color = colors.textSecondary,
            )
        }
    }
}

@Composable
private fun BottomNavBar(
    currentDestination: NavDestination,
    onSelectDestination: (NavDestination) -> Unit,
) {
    val colors = CalqueTheme.colors

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colors.surface,
        tonalElevation = 3.dp,
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for (dest in NavDestination.entries) {
                val isSelected = dest == currentDestination
                Box(
                    modifier =
                        Modifier.weight(1f)
                            .clickable { onSelectDestination(dest) }
                            .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = dest.title,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color =
                            if (isSelected) {
                                colors.slateBlueAccent
                            } else {
                                colors.textSecondary
                            },
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaceholderScreen(content: String) {
    val colors = CalqueTheme.colors

    Box(
        modifier = Modifier.fillMaxSize().background(colors.background).padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = content,
            fontSize = 16.sp,
            fontFamily = FontFamily.Serif,
            color = colors.textPrimary,
        )
    }
}
