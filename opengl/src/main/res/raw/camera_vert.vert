attribute vec4 vPosition; //变量 float[4]  一个顶点  java传过来的 attribute:输入变量in

attribute vec2 vCoord;  //纹理坐标

varying vec2 aCoord; // varying 从顶点着色器传递到片元着色器的数据变量

uniform mat4 vMatrix;

void main(){
    //内置变量： 把坐标点赋值给gl_position 就Ok了。
    gl_Position = vPosition;
    aCoord = (vMatrix * vec4(vCoord,1.0,1.0)).xy;
}

//attribute 
//属性变量，顶点着色器输入数据。如：顶点坐标、纹理坐标、颜色等。
//
//uniforms 
//一致变量。在着色器执行期间一致变量的值是不变的。类似常量，但不同的是，这个值在编译时期是未知的，由着色器外部初始化。
//
//varying 
//易变变量，顶点着色器输出数据。是从顶点着色器传递到片元着色器的数据变量。
