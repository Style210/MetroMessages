// File: RichMessageEditText.kt
package com.metromessages.ui.components

import android.content.Context
import android.text.InputType
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.toColorInt
import androidx.core.widget.addTextChangedListener

@Composable
fun RichMessageEditText(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    maxLines: Int,
    focusRequester: FocusRequester,
    onLineCountChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    AndroidView(
        factory = { context -> // ← FIXED: Use 'context' instead of 'ctx'
            createEditText(
                context = context, // ← FIXED: Parameter name should be 'context'
                maxLines = maxLines,
                onSend = onSend,
                focusManager = focusManager,
                onLineCountChange = onLineCountChange
            )
        },
        modifier = modifier
            .focusRequester(focusRequester)
            .clickable {
                focusRequester.requestFocus()
            },
        update = { editText ->
            // Update text if it changed from outside
            if (editText.text.toString() != text) {
                editText.setText(text)
                editText.setSelection(text.length)

                val newLineCount = editText.lineCount.coerceAtMost(maxLines)
                onLineCountChange(newLineCount)
            }

            // Update text change listener
            editText.addTextChangedListener {
                it?.let { editable ->
                    if (editable.toString() != text) {
                        onTextChange(editable.toString())

                        val newLineCount = editText.lineCount.coerceAtMost(maxLines)
                        onLineCountChange(newLineCount)
                    }
                }
            }
        }
    )
}

private fun createEditText(
    context: Context, // ← FIXED: Parameter name should be 'context'
    maxLines: Int,
    onSend: () -> Unit,
    focusManager: FocusManager? = null,
    onLineCountChange: (Int) -> Unit
): EditText {
    return EditText(context).apply {
        // ... rest of the function remains exactly the same ...
        // ENABLE RICH CONTENT SUPPORT
        setRawInputType(InputType.TYPE_CLASS_TEXT)
        isFocusable = true
        isFocusableInTouchMode = true
        isLongClickable = true
        setTextIsSelectable(true)

        // Enable rich content (GIFs, stickers, etc.)
        inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                InputType.TYPE_TEXT_FLAG_CAP_SENTENCES

        // Enable IME options for rich content
        imeOptions = EditorInfo.IME_ACTION_SEND or
                EditorInfo.IME_FLAG_NO_EXTRACT_UI

        // Use property assignment syntax
        this.maxLines = maxLines
        this.isVerticalScrollBarEnabled = true

        // Handle send action
        setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                onSend()
                true
            } else {
                false
            }
        }

        // Handle focus - show keyboard when focused
        setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                post {
                    // Request focus and show keyboard
                    requestFocus()
                    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                    imm.showSoftInput(this, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)

                    // Update line count when gaining focus
                    val currentLineCount = lineCount.coerceAtMost(maxLines)
                    onLineCountChange(currentLineCount)
                }
            } else {
                // Update line count when losing focus
                val currentLineCount = lineCount.coerceAtMost(maxLines)
                onLineCountChange(currentLineCount)
            }
        }

        // Click listener to request focus
        setOnClickListener {
            requestFocus()
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.showSoftInput(this, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)

            // Update line count on click
            val currentLineCount = lineCount.coerceAtMost(maxLines)
            onLineCountChange(currentLineCount)
        }

        // Text changed listener for line count tracking
        addTextChangedListener {
            val currentLineCount = lineCount.coerceAtMost(maxLines)
            onLineCountChange(currentLineCount)
        }

        // Initial line count
        post {
            val initialLineCount = lineCount.coerceAtMost(maxLines)
            onLineCountChange(initialLineCount)
        }

        // Styling (match your app's theme)
        setTextColor(android.graphics.Color.WHITE)
        setHintTextColor("#80FFFFFF".toColorInt())
        hint = "Type a message..."
        isFocusable = true
        isFocusableInTouchMode = true

        // Remove default background and padding
        background = null
        setPadding(
            resources.getDimensionPixelSize(android.R.dimen.app_icon_size) / 4,
            resources.getDimensionPixelSize(android.R.dimen.app_icon_size) / 7,
            resources.getDimensionPixelSize(android.R.dimen.app_icon_size) / 4,
            resources.getDimensionPixelSize(android.R.dimen.app_icon_size) / 4
        )
    }
}

