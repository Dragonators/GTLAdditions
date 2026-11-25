attribute float aSize;attribute vec4 aCol;uniform float uScale;varying vec4 vCol;
void main(){vec4 mv=modelViewMatrix*vec4(position,1.);gl_Position=projectionMatrix*mv;float s=aSize*uScale/max(.1,-mv.z);vCol=aCol;
  if(s<2.5){vCol.a*=s/2.5;s=2.5;}gl_PointSize=min(s,900.);}