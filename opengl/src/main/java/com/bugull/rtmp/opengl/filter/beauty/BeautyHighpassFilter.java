package com.bugull.rtmp.opengl.filter.beauty;

import android.content.Context;
import android.opengl.GLES20;

import com.bugull.rtmp.opengl.R;
import com.bugull.rtmp.opengl.filter.AbstractFboFilter;
import com.bugull.rtmp.opengl.filter.FilterChain;


public class BeautyHighpassFilter extends AbstractFboFilter {
    private int vBlurTexture;
    private int blurTexture;

    public BeautyHighpassFilter(Context context) {
        super(context, R.raw.base_vert, R.raw.beauty_highpass,"BeautyHighpassFilter");
        vBlurTexture = GLES20.glGetUniformLocation(getProgram(), "vBlurTexture");
    }

    @Override
    public void onBeforeDraw(FilterChain.FilterChainContext filterContext) {
        super.onBeforeDraw(filterContext);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, blurTexture);
        GLES20.glUniform1i(vBlurTexture, 1);
    }

    public void setBlurTexture(int blurTexture) {
        this.blurTexture = blurTexture;
    }
}
