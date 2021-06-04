#extension GL_OES_EGL_image_external : require
//摄像头数据比较特殊的一个地方 外部纹理
// 其他的用 sampler2D
precision mediump float; // 数据精度
varying vec2 aCoord;

uniform samplerExternalOES  vTexture;  // samplerExternalOES: 图片， 采样器 uniform输入

void main(){
    //  texture2D: vTexture采样器，采样  aCoord 这个像素点的RGBA值
    vec4 rgba = texture2D(vTexture,aCoord);  //rgba
    gl_FragColor = vec4(rgba.r,rgba.g,rgba.b,rgba.a);
//    gl_FragColor = vec4(1.-rgba.r,1.-rgba.g,1.-rgba.b,rgba.a);

 /*   float r = 0.33*rgba.a+0.59*rgba.g+0.11*rgba.b;
    gl_FragColor = vec4(r,r,r,rgba.a);*/ //hui du hua

}


//attribute 
//属性变量，顶点着色器输入数据。如：顶点坐标、纹理坐标、颜色等。
//
//uniforms 
//一致变量。在着色器执行期间一致变量的值是不变的。类似常量，但不同的是，这个值在编译时期是未知的，由着色器外部初始化。
//
//varying 
//易变变量，顶点着色器输出数据。是从顶点着色器传递到片元着色器的数据变量。
