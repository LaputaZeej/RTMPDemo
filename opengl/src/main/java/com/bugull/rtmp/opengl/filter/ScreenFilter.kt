package com.bugull.rtmp.opengl.filter

import android.content.Context
import com.bugull.rtmp.opengl.R

/**
 * Author by xpl, Date on 2021/6/3.
 * 着色器 sampler2D
 */
class ScreenFilter(ctx: Context) :
    AbstractFilter(ctx, R.raw.base_vert, R.raw.base_frag,"ScreenFilter")