package io.homeassistant.companion.android.assist.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikepenz.iconics.compose.Image
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import io.homeassistant.companion.android.R
import io.homeassistant.companion.android.common.assist.AssistViewModelBase
import kotlinx.coroutines.launch
import io.homeassistant.companion.android.common.R as commonR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistSheetView(
    conversation: List<AssistMessage>,
    pipelines: List<AssistUiPipeline>,
    inputMode: AssistViewModelBase.AssistInputMode?,
    currentPipeline: AssistUiPipeline?,
    fromFrontend: Boolean,
    onSelectPipeline: (Int, String) -> Unit,
    onManagePipelines: (() -> Unit)?,
    onChangeInput: () -> Unit,
    onTextInput: (String) -> Unit,
    onMicrophoneInput: () -> Unit,
    onHide: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val state = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = {
            if (it == SheetValue.Hidden) {
                coroutineScope.launch { onHide() }
            }
            true
        }
    )
    val configuration = LocalConfiguration.current

    val sheetCornerRadius = dimensionResource(R.dimen.bottom_sheet_corner_radius)

    ModalBottomSheet(
        sheetState = state,
        shape = RoundedCornerShape(topStart = sheetCornerRadius, topEnd = sheetCornerRadius),
        scrimColor = Color.Transparent,
        modifier = Modifier.fillMaxSize(),
        onDismissRequest = { onHide() },
        content = {
            Box(
                Modifier
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(16.dp)
            ) {
                Column {
                    val lazyListState = rememberLazyListState()
                    LaunchedEffect("${conversation.size}.${conversation.lastOrNull()?.message?.length}") {
                        lazyListState.animateScrollToItem(conversation.size)
                    }

                    AssistSheetHeader(
                        pipelines = pipelines,
                        currentPipeline = currentPipeline,
                        fromFrontend = fromFrontend,
                        onSelectPipeline = onSelectPipeline,
                        onManagePipelines = onManagePipelines
                    )
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier
                            .padding(vertical = 16.dp)
                            .heightIn(
                                max = configuration.screenHeightDp.dp -
                                    WindowInsets.safeContent.asPaddingValues().calculateBottomPadding() -
                                    WindowInsets.safeContent.asPaddingValues().calculateTopPadding() -
                                    96.dp
                            )
                    ) {
                        items(conversation) {
                            SpeechBubble(text = it.message, isResponse = !it.isInput)
                        }
                    }
                    AssistSheetControls(
                        inputMode,
                        onChangeInput,
                        onTextInput,
                        onMicrophoneInput
                    )
                }
            }
        }
    )
}

@Composable
fun AssistSheetHeader(
    pipelines: List<AssistUiPipeline>,
    currentPipeline: AssistUiPipeline?,
    fromFrontend: Boolean,
    onSelectPipeline: (Int, String) -> Unit,
    onManagePipelines: (() -> Unit)?
) = Column(verticalArrangement = Arrangement.Center) {
    Text(
        text = stringResource(if (fromFrontend) commonR.string.assist else commonR.string.app_name),
        fontSize = 20.sp,
        letterSpacing = 0.25.sp
    )
    if (currentPipeline != null) {
        val color = colorResource(commonR.color.colorOnSurfaceVariant)

        Row(modifier = Modifier.fillMaxWidth()) {
            Box {
                var pipelineShowList by remember { mutableStateOf(false) }
                val pipelineShowServer by rememberSaveable(pipelines.size) {
                    mutableStateOf(pipelines.distinctBy { it.serverId }.size > 1)
                }
                Row(
                    modifier = Modifier.clickable { pipelineShowList = !pipelineShowList }
                ) {
                    Text(
                        text = if (pipelineShowServer) "${currentPipeline.serverName}: ${currentPipeline.name}" else currentPipeline.name,
                        color = color,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = stringResource(commonR.string.assist_change_pipeline),
                        modifier = Modifier
                            .height(16.dp)
                            .padding(start = 4.dp),
                        tint = color
                    )
                }
                DropdownMenu(
                    expanded = pipelineShowList,
                    onDismissRequest = { pipelineShowList = false }
                ) {
                    pipelines.forEach {
                        val isSelected =
                            it.serverId == currentPipeline.serverId && it.id == currentPipeline.id
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = if (pipelineShowServer) "${it.serverName}: ${it.name}" else it.name,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else null
                                )
                            },
                            onClick = {
                                onSelectPipeline(it.serverId, it.id)
                                pipelineShowList = false
                            }
                        )
                    }
                    if (onManagePipelines != null) {
                        Divider()
                        DropdownMenuItem(
                            text = { Text(stringResource(commonR.string.assist_manage_pipelines)) },
                            onClick = { onManagePipelines() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AssistSheetControls(
    inputMode: AssistViewModelBase.AssistInputMode?,
    onChangeInput: () -> Unit,
    onTextInput: (String) -> Unit,
    onMicrophoneInput: () -> Unit
) = Row(verticalAlignment = Alignment.CenterVertically) {
    if (inputMode == null) { // Pipeline info has not yet loaded, empty space for now
        Spacer(modifier = Modifier.height(64.dp))
        return
    }

    if (inputMode == AssistViewModelBase.AssistInputMode.BLOCKED) { // No info and not recoverable, no space
        return
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(inputMode) {
        if (inputMode == AssistViewModelBase.AssistInputMode.TEXT || inputMode == AssistViewModelBase.AssistInputMode.TEXT_ONLY) {
            focusRequester.requestFocus()
        }
    }

    if (inputMode == AssistViewModelBase.AssistInputMode.TEXT || inputMode == AssistViewModelBase.AssistInputMode.TEXT_ONLY) {
        var text by rememberSaveable(stateSaver = TextFieldValue.Saver) {
            mutableStateOf(TextFieldValue())
        }
        TextField(
            value = text,
            onValueChange = { text = it },
            label = { Text(stringResource(commonR.string.assist_enter_a_request)) },
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = {
                if (text.text.isNotBlank()) {
                    onTextInput(text.text)
                    text = TextFieldValue("")
                }
            })
        )
        IconButton(
            onClick = {
                if (text.text.isNotBlank()) {
                    onTextInput(text.text)
                    text = TextFieldValue("")
                } else if (inputMode != AssistViewModelBase.AssistInputMode.TEXT_ONLY) {
                    onChangeInput()
                }
            },
            enabled = (inputMode != AssistViewModelBase.AssistInputMode.TEXT_ONLY || text.text.isNotBlank())
        ) {
            val inputIsSend = text.text.isNotBlank() || inputMode == AssistViewModelBase.AssistInputMode.TEXT_ONLY
            Image(
                asset = if (inputIsSend) CommunityMaterial.Icon3.cmd_send else CommunityMaterial.Icon3.cmd_microphone,
                contentDescription = stringResource(
                    if (inputIsSend) commonR.string.assist_send_text else commonR.string.assist_start_listening
                ),
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
                modifier = Modifier.size(24.dp)
            )
        }
    } else {
        Spacer(Modifier.size(48.dp))
        Spacer(Modifier.weight(0.5f))
        Box(
            modifier = Modifier.size(64.dp),
            contentAlignment = Alignment.Center
        ) {
            val inputIsActive = inputMode == AssistViewModelBase.AssistInputMode.VOICE_ACTIVE
            if (inputIsActive) {
                val transition = rememberInfiniteTransition()
                val scale by transition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.2f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(600, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    )
                )
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .scale(scale)
                        .background(color = colorResource(commonR.color.colorSpeechText), shape = CircleShape)
                        .clip(CircleShape)
                )
            }
            OutlinedButton(
                onClick = { onMicrophoneInput() },
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                border = if (inputIsActive) null else ButtonDefaults.outlinedButtonBorder,
                colors = if (inputIsActive) {
                    ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent, contentColor = Color.Black)
                } else {
                    ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                },
                contentPadding = PaddingValues(all = 0.dp)
            ) {
                Image(
                    asset = CommunityMaterial.Icon3.cmd_microphone,
                    contentDescription = stringResource(
                        if (inputIsActive) commonR.string.assist_stop_listening else commonR.string.assist_start_listening
                    ),
                    colorFilter = ColorFilter.tint(LocalContentColor.current),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(Modifier.weight(0.5f))
        IconButton({ onChangeInput() }) {
            Icon(
                imageVector = Icons.Outlined.Keyboard,
                contentDescription = stringResource(commonR.string.assist_enter_text),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun SpeechBubble(text: String, isResponse: Boolean) {
    Row(
        horizontalArrangement = if (isResponse) Arrangement.Start else Arrangement.End,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (isResponse) 0.dp else 24.dp,
                end = if (isResponse) 24.dp else 0.dp,
                top = 8.dp,
                bottom = 8.dp
            )
    ) {
        Box(
            modifier = Modifier
                .background(
                    if (isResponse) {
                        colorResource(commonR.color.colorAccent)
                    } else {
                        colorResource(commonR.color.colorSpeechText)
                    },
                    AbsoluteRoundedCornerShape(
                        topLeft = 12.dp,
                        topRight = 12.dp,
                        bottomLeft = if (isResponse) 0.dp else 12.dp,
                        bottomRight = if (isResponse) 12.dp else 0.dp
                    )
                )
                .padding(4.dp)
        ) {
            Text(
                text = text,
                color = if (isResponse) {
                    Color.White
                } else {
                    Color.Black
                },
                modifier = Modifier
                    .padding(2.dp)
            )
        }
    }
}
