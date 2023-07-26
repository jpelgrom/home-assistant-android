package io.homeassistant.companion.android.settings.vehicle

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.accompanist.themeadapter.material3.Mdc3Theme
import dagger.hilt.android.AndroidEntryPoint
import io.homeassistant.companion.android.R
import io.homeassistant.companion.android.common.data.servers.ServerManager
import io.homeassistant.companion.android.settings.vehicle.views.AndroidAutoFavoritesSettings
import javax.inject.Inject
import io.homeassistant.companion.android.common.R as commonR

@AndroidEntryPoint
class ManageAndroidAutoSettingsFragment : Fragment() {

    @Inject
    lateinit var serverManager: ServerManager

    val viewModel: ManageAndroidAutoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    @Deprecated("Deprecated in Java")
    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)

        menu.findItem(R.id.get_help)?.let {
            it.isVisible = true
            it.intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://companion.home-assistant.io/docs/android-auto"))
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                Mdc3Theme {
                    AndroidAutoFavoritesSettings(
                        androidAutoViewModel = viewModel,
                        serversList = serverManager.defaultServers,
                        defaultServer = serverManager.getServer()?.id ?: 0
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        activity?.title =
            if (requireContext().packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)) {
                getString(commonR.string.android_automotive_favorites)
            } else {
                getString(commonR.string.aa_favorites)
            }
    }
}
