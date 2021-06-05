precision mediump float;// 数据精度
varying vec2 aCoord;//当前要上颜色的像素点

uniform sampler2D  vTexture;//图片
uniform vec2 left_eye;//float[2] -> x,y
uniform vec2 right_eye;
uniform vec2 scale;
uniform float scale2;


//r:要画的点与眼睛的距离
//rmax: 最大作用半径
// 0.4： 放大系数：-1-》1 大于0就是放大
float fs(float r, float rmax, float scale){
    return (1.0-pow(r/rmax-1.0, 2.0)*scale);
}

vec2 newCoord(vec2 coord, vec2 eye, float rmax, float scale){
    vec2 p = coord;
    //得到要画的点coord 与眼睛 eye的距离
    float r = distance(coord, eye);
    if (r < rmax){
        //缩放后的点 与眼睛的距离
        float fsr = fs(r, rmax, scale);
        // (缩放后的点 - 眼睛点坐标) / (原点-眼睛点坐标) = fsr/r
        //   (缩放后的点 - 眼睛点坐标) = fsr
        //   (原点-眼睛点坐标)  =  r
        // 缩放后的点 = fsr/r * (原点-眼睛点坐标) + 眼睛点坐标
        p = fsr * (coord-eye)  +eye;
    }
    return p;
}

void main(){
    //gl_FragColor = texture2D(vTexture,aCoord);
    float rmax = distance(left_eye, right_eye)/2.0;
    //float scale_01= scale.xy.x;
    float scale_01= scale2;
    vec2 p = newCoord(aCoord, left_eye, rmax, scale_01);
    p = newCoord(p, right_eye, rmax, scale_01);
    gl_FragColor = texture2D(vTexture, p);

}