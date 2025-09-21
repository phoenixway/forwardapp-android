import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.R
import com.romankozak.forwardappmobile.ui.screens.backlog.components.inputpanel.InputMode
import com.romankozak.forwardappmobile.ui.screens.backlog.components.inputpanel.NavPanelActions
import com.romankozak.forwardappmobile.ui.screens.backlog.components.inputpanel.NavPanelState

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun NavControls(state: NavPanelState, actions: NavPanelActions, contentColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (state.inputMode == InputMode.SearchInList) {
            IconButton(onClick = actions.onCloseSearch, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.Default.Close,
                    "Ð—Ð°ÐºÑ€Ð¸Ñ‚Ð¸ Ð¿Ð¾ÑˆÑƒÐº",
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        val backButtonAlpha by animateFloatAsState(
            if (state.canGoForward) 1f else 0.4f,
            label = "forwardAlpha"
        )
        IconButton(
            onClick = actions.onBackClick,
            enabled = state.canGoBack,
            modifier = Modifier
                .size(40.dp)
                .alpha(backButtonAlpha)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                stringResource(R.string.forward),
                tint = if (state.canGoForward) contentColor else contentColor.copy(alpha = 0.38f),
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))
        IconButton(onClick = actions.onHomeClick, modifier = Modifier.size(40.dp)) {
            Icon(
                Icons.Default.Home,
                stringResource(R.string.go_to_home_list),
                tint = contentColor.copy(alpha = 0.4f),
                modifier = Modifier.size(18.dp)
            )
        }

        IconButton(onClick = actions.onRevealInExplorer, modifier = Modifier.size(40.dp)) {
            Icon(
                Icons.Outlined.Visibility,
                "RevealInExplorer",
                tint = contentColor.copy(alpha = 0.4f),
                modifier = Modifier.size(18.dp)
            )
        }

        IconButton(onClick = actions.onRecentsClick, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector = Icons.Default.Restore,
                contentDescription = "ÐÐµÐ´Ð°Ð²Ð½Ñ–",
                tint = contentColor.copy(alpha = 0.4f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}