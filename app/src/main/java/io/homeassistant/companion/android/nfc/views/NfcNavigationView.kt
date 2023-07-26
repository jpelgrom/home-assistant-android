package io.homeassistant.companion.android.nfc.views

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.homeassistant.companion.android.nfc.NfcSetupActivity
import io.homeassistant.companion.android.nfc.NfcViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import io.homeassistant.companion.android.common.R as commonR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoadNfcView(
    viewModel: NfcViewModel,
    startDestination: String,
    pressedUpAtRoot: () -> Unit
) {
    val context = LocalContext.current

    val navController = rememberNavController()
    val canNavigateUp = remember { mutableStateOf(false) }
    navController.addOnDestinationChangedListener { controller, destination, _ ->
        canNavigateUp.value = controller.previousBackStackEntry != null
        viewModel.setDestination(destination.route)
    }
    LaunchedEffect("navigation") {
        viewModel.navigator.flow.onEach {
            navController.navigate(it.id) {
                if (it.popBackstackTo != null) {
                    popUpTo(it.popBackstackTo) { inclusive = it.popBackstackInclusive }
                }
            }
        }.launchIn(this)
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect("snackbar") {
        viewModel.nfcResultSnackbar.onEach {
            if (it != 0) {
                snackbarHostState.showSnackbar(context.getString(it))
            }
        }.launchIn(this)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(commonR.string.nfc_title_settings)) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (canNavigateUp.value) {
                            navController.navigateUp()
                        } else {
                            pressedUpAtRoot()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Outlined.ArrowBack,
                            contentDescription = stringResource(commonR.string.navigate_up)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://companion.home-assistant.io/docs/integrations/universal-links"))
                        context.startActivity(intent)
                    }) {
                        Icon(
                            imageVector = Icons.Outlined.HelpOutline,
                            contentDescription = stringResource(commonR.string.get_help),
                            tint = colorResource(commonR.color.colorOnBackground)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorResource(id = commonR.color.colorBackground)
                )
            )
        }
    ) { contentPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(contentPadding)
        ) {
            composable(NfcSetupActivity.NAV_WELCOME) {
                NfcWelcomeView(
                    isNfcEnabled = viewModel.isNfcEnabled,
                    onReadClicked = { viewModel.navigator.navigateTo(NfcSetupActivity.NAV_READ) },
                    onWriteClicked = { viewModel.writeNewTag() }
                )
            }
            composable(NfcSetupActivity.NAV_READ) {
                NfcReadView()
            }
            composable(NfcSetupActivity.NAV_WRITE) {
                NfcWriteView(
                    isNfcEnabled = viewModel.isNfcEnabled,
                    identifier = viewModel.nfcTagIdentifier,
                    onSetIdentifier = if (viewModel.nfcIdentifierIsEditable) {
                        { viewModel.setTagIdentifier(it) }
                    } else {
                        null
                    }
                )
            }
            composable(NfcSetupActivity.NAV_EDIT) {
                NfcEditView(
                    identifier = viewModel.nfcTagIdentifier,
                    showDeviceSample = viewModel.usesAndroidDeviceId,
                    onDuplicateClicked = { viewModel.duplicateNfcTag() },
                    onFireEventClicked = { viewModel.fireNfcTagEvent() }
                )
            }
        }
    }
}
