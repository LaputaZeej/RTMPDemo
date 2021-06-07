package com.bugull.rtmp.opengl.filter.abs

import android.content.Context
import android.opengl.GLES20
import com.bugull.rtmp.opengl.R
import com.bugull.rtmp.opengl.filter.AbstractFboFilter
import com.bugull.rtmp.opengl.filter.FilterChain

/**
 * Author by xpl, Date on 2021/6/5.
 */
class SoulFilter(
    ctx: Context,
    name: String = "SoulFilter",
) : AbstractFboFilter(ctx,
    vertexShaderId = R.raw.base_vert,
    fragmentShaderId = R.raw.soul_frag,
    name) {

    private val mixturePercent: Int
    private val scalePercent: Int

    var mix = 0.0f//透明度，越大越透明
    var scale = 0.0f//缩放，越大就放的越大

    init {
        mixturePercent = GLES20.glGetUniformLocation(program, "mixturePercent")
        scalePercent = GLES20.glGetUniformLocation(program, "scalePercent")
    }

    override fun onBeforeDraw(chainContext: FilterChain.FilterChainContext) {
        super.onBeforeDraw(chainContext)

        GLES20.glUniform1f(mixturePercent, 1.0f - mix)
        GLES20.glUniform1f(scalePercent, scale + 1.0f)
        mix += 0.08f
        scale += 0.08f
        if (mix >= 1.0) {
            mix = 0.0f
        }
        if (scale >= 1.0) {
            scale = 0.0f
        }
    }
}