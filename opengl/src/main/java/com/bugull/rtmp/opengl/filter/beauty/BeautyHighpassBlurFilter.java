package com.bugull.rtmp.opengl.filter.beauty;

import android.content.Context;
import android.opengl.GLES20;

import com.bugull.rtmp.opengl.R;
import com.bugull.rtmp.opengl.filter.AbstractFboFilter;
import com.bugull.rtmp.opengl.filter.FilterChain;


public class BeautyHighpassBlurFilter extends AbstractFboFilter {
    private int widthIndex;
    private int heightIndex;

    public BeautyHighpassBlurFilter(Context context) {
        super(context, R.raw.base_vert, R.raw.beauty_highpass_blur,"BeautyHighpassBlurFilter");
        widthIndex = GLES20.glGetUniformLocation(getProgram(), "width");
        heightIndex = GLES20.glGetUniformLocation(getProgram(), "height");
    }

    @Override
    public void onBeforeDraw(FilterChain.FilterChainContext filterContext) {
        super.onBeforeDraw(filterContext);
        GLES20.glUniform1i(widthIndex, filterContext.getWidth());
        GLES20.glUniform1i(heightIndex, filterContext.getHeight());
    }

}
