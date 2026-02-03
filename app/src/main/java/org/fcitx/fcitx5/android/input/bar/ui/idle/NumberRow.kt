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
class NumberRow(ctx: Context, theme: Theme) : BaseKeyboard(ctx, theme, Layout) {

    private var gestureStartEvent: MotionEvent? = null
    private var collapseGestureTriggerd: Boolean = false

    var onCollapseListener: (() -> Unit)? = null

    // return true if the swipe distance is enough to trigger collapse
    var shouldCollapse: (start: PointF, current: PointF) -> Boolean = { _, _ -> false }

    private fun checkGesture(ev: MotionEvent): Boolean {
        val startEvent = gestureStartEvent ?: return false
        val firstPointerId = startEvent.getPointerId(startEvent.actionIndex)
        if (ev.getPointerId(ev.actionIndex) == firstPointerId) {
            val start = PointF(startEvent.x, startEvent.y)
            val current = PointF(ev.getX(ev.actionIndex), ev.getY(ev.actionIndex))
            if (shouldCollapse(start, current)) {
                Timber.d("NumberRow: intercepted gesture from child keyboard to handle swipe")
                resetState()
                collapseGestureTriggerd = true
                return true
            }
        }
        return false
    }

    private fun resetState() {
        gestureStartEvent?.recycle()
        gestureStartEvent = null
        collapseGestureTriggerd = false
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> gestureStartEvent = MotionEvent.obtain(ev)
            MotionEvent.ACTION_MOVE -> {
                if (checkGesture(ev)) return true
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> resetState()
        }
        return super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        var handled = false
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> gestureStartEvent = MotionEvent.obtain(ev)
            MotionEvent.ACTION_MOVE -> checkGesture(ev)
            MotionEvent.ACTION_UP -> {
                if (collapseGestureTriggerd) {
                    resetState()
                    onCollapseListener?.invoke()
                    handled = true
                }
            }
            MotionEvent.ACTION_CANCEL -> resetState()
        }
        return super.onTouchEvent(ev) || handled
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
