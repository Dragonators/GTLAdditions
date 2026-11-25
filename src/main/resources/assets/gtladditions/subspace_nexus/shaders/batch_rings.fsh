uniform float uGlow;varying float vA;varying vec3 vCol;varying float vU;varying vec4 vS;varying float vF;varying float vRot;varying vec2 vArc;
void main(){
  float u=fract(vU-vArc.x);if(u>vArc.y)discard;
  if(vS.x>.5){float s=fract(vU*vS.x+vRot);if(s>1.-vS.y)discard;}
  if(vS.z>.5){float d=fract(vU*vS.z+vRot*vS.z*.0);if(d>vS.w)discard;}
  float a=vA*(.3+.7*vF)*uGlow;
  gl_FragColor=vec4(vCol*a,a);
}