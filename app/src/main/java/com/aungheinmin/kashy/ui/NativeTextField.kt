package com.aungheinmin.kashy.ui

import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.compose.foundation.clickable
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Lets a caller move focus into a [NativeTextField] or bring its keyboard back. */
class NativeTextFieldController {
    internal var editText: EditText? = null

    fun focus() {
        val view = editText ?: return
        view.requestFocus()
        view.setSelection(view.text.length)
        showKeyboard()
    }

    fun showKeyboard() {
        val view = editText ?: return
        view.post { view.context.getSystemService(InputMethodManager::class.java)?.showSoftInput(view, 0) }
    }
}

/**
 * An outlined text field backed by a platform [EditText].
 *
 * Burmese keyboards such as Frozen and Gboard's Myanmar layout edit the word in progress through the composing
 * region, and Compose's own text field can stop taking input after a backspace there; EditText is what those
 * keyboards are built against.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NativeTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    suffix: String? = null,
    isError: Boolean = false,
    supportingText: String? = null,
    inputType: Int = InputType.TYPE_CLASS_TEXT,
    imeAction: Int = EditorInfo.IME_ACTION_DONE,
    onImeAction: (() -> Unit)? = null,
    controller: NativeTextFieldController? = null,
    onFocusChange: (Boolean) -> Unit = {},
) {
    val interactionSource = remember { MutableInteractionSource() }
    var focus by remember { mutableStateOf<FocusInteraction.Focus?>(null) }
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentOnFocusChange by rememberUpdatedState(onFocusChange)
    val currentOnImeAction by rememberUpdatedState(onImeAction)
    val colors = MaterialTheme.colorScheme
    val textStyle = MaterialTheme.typography.bodyLarge
    val textSizePx = with(LocalDensity.current) { textStyle.fontSize.toPx() }
    val cursorWidthPx = with(LocalDensity.current) { 2.dp.roundToPx() }

    val fieldController = controller ?: remember { NativeTextFieldController() }
    val bringIntoView = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()

    Box(
        modifier
            .defaultMinSize(minHeight = OutlinedTextFieldDefaults.MinHeight)
            .bringIntoViewRequester(bringIntoView)
            .clickable(interactionSource = null, indication = null) { fieldController.focus() },
    ) {
        OutlinedTextFieldDefaults.DecorationBox(
            value = value,
            innerTextField = {
                AndroidView(
                    factory = { context ->
                        EditText(context).apply {
                            background = null
                            setPadding(0, 0, 0, 0)
                            gravity = Gravity.CENTER_VERTICAL or Gravity.START
                            isSingleLine = true
                            setText(value)
                            addTextChangedListener(object : TextWatcher {
                                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
                                override fun afterTextChanged(s: Editable?) = currentOnValueChange(s?.toString().orEmpty())
                            })
                            setOnFocusChangeListener { _, hasFocus ->
                                if (hasFocus) {
                                    FocusInteraction.Focus().also { focus = it; interactionSource.tryEmit(it) }
                                    // Compose doesn't scroll to a focused platform view, and the keyboard is still opening here.
                                    scope.launch {
                                        delay(350)
                                        bringIntoView.bringIntoView()
                                    }
                                } else {
                                    focus?.let { interactionSource.tryEmit(FocusInteraction.Unfocus(it)) }
                                    focus = null
                                }
                                currentOnFocusChange(hasFocus)
                            }
                            setOnEditorActionListener { _, actionId, _ ->
                                val action = currentOnImeAction
                                if (action != null && actionId == imeOptions) {
                                    action()
                                    true
                                } else false
                            }
                        }
                    },
                    update = { view ->
                        fieldController.editText = view
                        if (view.inputType != inputType) view.inputType = inputType
                        view.imeOptions = imeAction
                        view.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizePx)
                        view.setTextColor(colors.onSurface.toArgb())
                        view.highlightColor = colors.primary.copy(alpha = 0.3f).toArgb()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            view.textCursorDrawable = GradientDrawable().apply {
                                setColor((if (isError) colors.error else colors.primary).toArgb())
                                setSize(cursorWidthPx, view.lineHeight)
                            }
                        }
                        if (view.text.toString() != value) {
                            view.setText(value)
                            view.setSelection(value.length)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            enabled = true,
            singleLine = true,
            visualTransformation = VisualTransformation.None,
            interactionSource = interactionSource,
            isError = isError,
            label = { Text(label) },
            suffix = suffix?.let { { Text(it) } },
            supportingText = supportingText?.let { { Text(it) } },
        )
    }
}
