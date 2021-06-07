package com.bugull.rtmp.opengl.filter

import android.content.Context
import com.bugull.rtmp.opengl.R

/**
 * 分三屏
 */
class SplitFilter(ctx: Context) :
    AbstractFboFilter(ctx, R.raw.base_vert, R.raw.split3_screen,"SplitFilter")