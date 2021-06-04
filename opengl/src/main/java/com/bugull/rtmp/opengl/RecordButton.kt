package com.bugull.rtmp.opengl

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent

/**
 * Author by xpl, Date on 2021/6/3.
 */
class RecordButton @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0,
) : androidx.appcompat.widget.AppCompatButton(context, attrs, defStyleAttr) {

    var onRecordListener: ((Boolean) -> Unit)? = null

    init {
        setText("长按录制")
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                setText("录制ing")
                onRecordListener?.invoke(true)
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                setText("长按录制")
                onRecordListener?.invoke(false)
                return true
            }
        }
        return super.onTouchEvent(event)
    }


}