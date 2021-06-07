package com.bugull.rtmp.opengl.filter.beauty;

import android.content.Context;
import android.opengl.GLES20;

import com.bugull.rtmp.opengl.R;
import com.bugull.rtmp.opengl.filter.AbstractFboFilter;
import com.bugull.rtmp.opengl.filter.FilterChain;

public class BeautyAdjustFilter extends AbstractFboFilter {

    private int level;
    private int vBlurTexture;
    private int vHighpassBlurTexture;

    private int blurTexture;
    private int highpassBlurTexture;

    public BeautyAdjustFilter(Context context) {
        super(context, R.raw.base_vert, R.raw.beauty_adjust,"BeautyAdjustFilter");
        level = GLES20.glGetUniformLocation(getProgram(), "level");
        vBlurTexture = GLES20.glGetUniformLocation(getProgram(), "blurTexture");
        vHighpassBlurTexture = GLES20.glGetUniformLocation(getProgram(), "highpassBlurTexture");
    }



    @Override
    public void onBeforeDraw(FilterChain.FilterChainContext filterContext) {
        super.onBeforeDraw(filterContext);
        GLES20.glUniform1f(level, filterContext.getBeautyLevel());

        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, blurTexture);
        GLES20.glUniform1i(vBlurTexture, 1);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, highpassBlurTexture);
        GLES20.glUniform1i(vHighpassBlurTexture, 2);
    }

    public void setBlurTexture(int blurTexture) {
        this.blurTexture = blurTexture;
    }

    public void setHighpassBlurTexture(int highpassBlurTexture) {
        this.highpassBlurTexture = highpassBlurTexture;
    }
}