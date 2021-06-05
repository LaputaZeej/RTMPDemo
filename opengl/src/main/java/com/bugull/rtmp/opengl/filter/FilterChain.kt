package com.bugull.rtmp.opengl.filter

import android.util.Log
import com.bugull.rtmp.opengl.face.Face

/**
 * Author by xpl, Date on 2021/6/4.
 */
class FilterChain(
    val filters: List<AbstractFilter>,
    val index: Int,
    val chainContext: FilterChainContext = FilterChainContext(),
) {


    class FilterChainContext {
        var mtx: FloatArray = FloatArray(0)
        var face: Face? = null
        var width: Int = 0
        var height: Int = 0

        fun setSize(width: Int, height: Int) {
            this.width = width
            this.height = height
        }
    }


    fun proceed(textureId: Int): Int {
        //Log.i("_opengles_", "proceed ....$index >? ${filters.size} ")
        if (index >= filters.size) {
            return textureId
        }
        //Log.i("_opengles_", "proceed .... ")
        val i = index+1
        val filterChain = FilterChain(filters, i, chainContext)
        return filters[index].onDraw(textureId, filterChain)
    }


    fun release() {
        filters.forEach(AbstractFilter::release)
    }

    fun setSize(width: Int, height: Int) {
        this.chainContext.setSize(width, height)
    }

    fun setTransformMatrix(mtx: FloatArray) {
        this.chainContext.mtx = mtx
    }

    fun setFace(face: Face?){
        this.chainContext.face = face
    }


}