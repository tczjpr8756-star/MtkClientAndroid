package com.mtkclientandroid.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.ViewModule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mtkclientandroid.McCommand
import com.mtkclientandroid.engine.MtkViewModel
import com.mtkclientandroid.ui.commands.CommandsScreen
import com.mtkclientandroid.ui.console.ConsoleScreen
import com.mtkclientandroid.ui.files.DumpedFilesScreen
import com.mtkclientandroid.ui.settings.AboutScreen
import com.mtkclientandroid.ui.settings.AppearanceScreen
import com.mtkclientandroid.ui.settings.ProtocolsScreen
import com.mtkclientandroid.ui.settings.SettingsHomeScreen
import com.mtkclientandroid.ui.settings.TerminalSettingsScreen
import com.mtkclientandroid.ui.theme.MtkTheme

private enum class Tab { Console, Commands, Settings }

private sealed class SubScreen {
    data object None : SubScreen()
    data object Protocols : SubScreen()
    data object Appearance : SubScreen()
    data object Terminal : SubScreen()
    data object About : SubScreen()
    data object Files : SubScreen()
    data class Review(val command: McCommand) : SubScreen()
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MtkApp(vm: MtkViewModel = viewModel()) {
    val appearance by vm.appearance.collectAsState()
    val snackbarMessage by vm.snackbar.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }
    var tab by remember { mutableStateOf(Tab.Console) }
    var sub by remember { mutableStateOf<SubScreen>(SubScreen.None) }

    LaunchedEffect(snackbarMessage) {
        val msg = snackbarMessage ?: return@LaunchedEffect
        snackbarHost.showSnackbar(msg)
        vm.consumeSnackbar()
    }

    MtkTheme(appearance) {
        BackHandler(enabled = sub != SubScreen.None) { sub = SubScreen.None }
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHost) },
            bottomBar = {
                if (sub == SubScreen.None) {
                    NavigationBar {
                        NavigationBarItem(
                            selected = tab == Tab.Console,
                            onClick = { tab = Tab.Console },
                            icon = { Icon(Icons.Outlined.Terminal, contentDescription = null) },
                            label = { Text("Console") }
                        )
                        NavigationBarItem(
                            selected = tab == Tab.Commands,
                            onClick = { tab = Tab.Commands },
                            icon = { Icon(Icons.Outlined.ViewModule, contentDescription = null) },
                            label = { Text("Commands") }
                        )
                        NavigationBarItem(
                            selected = tab == Tab.Settings,
                            onClick = { tab = Tab.Settings },
                            icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                            label = { Text("Settings") }
                        )
                    }
                }
            }
        ) { padding ->
            val modifier = Modifier.padding(padding)
            when (val current = sub) {
                SubScreen.None -> when (tab) {
                    Tab.Console -> ConsoleScreen(
                        vm = vm,
                        modifier = modifier,
                        onOpenFiles = { sub = SubScreen.Files }
                    )
                    Tab.Commands -> CommandsScreen(
                        vm = vm,
                        modifier = modifier,
                        review = null,
                        onOpenReview = { sub = SubScreen.Review(it) },
                        onCloseReview = { }
                    )
                    Tab.Settings -> SettingsHomeScreen(
                        vm = vm,
                        modifier = modifier,
                        onProtocols = { sub = SubScreen.Protocols },
                        onAppearance = { sub = SubScreen.Appearance },
                        onTerminal = { sub = SubScreen.Terminal },
                        onAbout = { sub = SubScreen.About },
                        onFiles = { sub = SubScreen.Files }
                    )
                }
                SubScreen.Protocols -> ProtocolsScreen(vm, modifier) { sub = SubScreen.None }
                SubScreen.Appearance -> AppearanceScreen(vm, modifier) { sub = SubScreen.None }
                SubScreen.Terminal -> TerminalSettingsScreen(vm, modifier) { sub = SubScreen.None }
                SubScreen.About -> AboutScreen(modifier) { sub = SubScreen.None }
                SubScreen.Files -> DumpedFilesScreen(vm, modifier) { sub = SubScreen.None }
                is SubScreen.Review -> CommandsScreen(
                    vm = vm,
                    modifier = modifier,
                    review = current.command,
                    onOpenReview = { sub = SubScreen.Review(it) },
                    onCloseReview = { sub = SubScreen.None }
                )
            }
        }
    }
}
