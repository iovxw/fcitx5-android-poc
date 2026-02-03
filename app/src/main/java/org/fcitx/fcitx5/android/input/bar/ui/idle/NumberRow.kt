/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.bar.ui.idle

import android.annotation.SuppressLint
import android.content.Context
import org.fcitx.fcitx5.android.core.KeySym
import org.fcitx.fcitx5.android.data.theme.Theme
import android.graphics.PointF
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import org.fcitx.fcitx5.android.input.keyboard.BaseKeyboard
import org.fcitx.fcitx5.android.input.keyboard.KeyAction
import org.fcitx.fcitx5.android.input.keyboard.KeyActionListener
import org.fcitx.fcitx5.android.input.keyboard.KeyDef
import org.fcitx.fcitx5.android.input.keyboard.CustomGestureView
import org.fcitx.fcitx5.android.input.popup.PopupActionListener
import kotlin.math.abs
import timber.log.Timber

@SuppressLint("ViewConstructor")
class NumberRow(ctx: Context, theme: Theme) : CustomGestureView(ctx) {
    private val keyboard = object : BaseKeyboard(ctx, theme, Layout) {}

    private var gestureStartEvent: MotionEvent? = null

    var keyActionListener: KeyActionListener?
        get() = keyboard.keyActionListener
        set(value) {
            keyboard.keyActionListener = value
        }

    var popupActionListener: PopupActionListener?
        get() = keyboard.popupActionListener
        set(value) {
            keyboard.popupActionListener = value
        }

    init {
        addView(keyboard, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (!swipeEnabled) return false
        // Intercept touch events from the child keyboard when a gesture exceeds
        // the configured swipe thresholds.
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                gestureStartEvent = MotionEvent.obtain(ev)
            }

            MotionEvent.ACTION_MOVE -> {
                gestureStartEvent?.let { startEvent ->
                    val firstPointerId = startEvent.getPointerId(startEvent.actionIndex)
                    if (ev.getPointerId(ev.actionIndex) == firstPointerId) {
                        val dx = abs(ev.getX(ev.actionIndex) - startEvent.x)
                        val dy = abs(ev.getY(ev.actionIndex) - startEvent.y)
                        if (dx > swipeThresholdX || dy > swipeThresholdY) {
                            Timber.d("NumberRow: intercepted gesture from child keyboard to handle swipe")
                            // Initialize parent gesture state using the recorded DOWN
                            super.onTouchEvent(startEvent)
                            startEvent.recycle()
                            gestureStartEvent = null
                            return true
                        }
                    }
                }
            }

            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                gestureStartEvent?.recycle()
                gestureStartEvent = null
            }
        }

        return false
    }

    companion object {
        val Layout = listOf(
            listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0").map { digit ->
                KeyDef(
                    KeyDef.Appearance.Text(
                        displayText = digit,
                        textSize = 21f,
                        border = KeyDef.Appearance.Border.Off,
                        margin = false
                    ),
                    setOf(
                        KeyDef.Behavior.Press(KeyAction.SymAction(KeySym(digit.codePointAt(0))))
                    ),
                    arrayOf(KeyDef.Popup.Preview(digit))
                )
            }
        )
    }
}
