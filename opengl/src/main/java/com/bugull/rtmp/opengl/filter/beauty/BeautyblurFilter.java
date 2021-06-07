package com.bugull.rtmp.opengl.filter.beauty;

import android.content.Context;
import android.opengl.GLES20;

import com.bugull.rtmp.opengl.R;
import com.bugull.rtmp.opengl.filter.AbstractFboFilter;
import com.bugull.rtmp.opengl.filter.FilterChain;


public class BeautyblurFilter extends AbstractFboFilter {
    private int texelWidthOffset;
    private int texelHeightOffset;
    private float mTexelWidth;
    private float mTexelHeight;

    public BeautyblurFilter(Context context) {
        super(context, R.raw.base_vert, R.raw.beauty_blur,"BeautyblurFilter");
        texelWidthOffset = GLES20.glGetUniformLocation(getProgram(), "texelWidthOffset");
        texelHeightOffset = GLES20.glGetUniformLocation(getProgram(), "texelHeightOffset");
    }



    @Override
    public void onBeforeDraw(FilterChain.FilterChainContext filterContext) {
        super.onBeforeDraw(filterContext);
        GLES20.glUniform1f(texelWidthOffset, mTexelWidth);
        GLES20.glUniform1f(texelHeightOffset, mTexelHeight);
    }

    /**
     * 设置高斯模糊的宽高
     */
    public void setTexelOffsetSize(float width, float height) {
        mTexelWidth = width;
        mTexelHeight = height;
        if (mTexelWidth != 0) {
            mTexelWidth = 1.0f / mTexelWidth;
        } else {
            mTexelWidth = 0;
        }
        if (mTexelHeight != 0) {
            mTexelHeight = 1.0f / mTexelHeight;
        } else {
            mTexelHeight = 0;
        }
    }
}
