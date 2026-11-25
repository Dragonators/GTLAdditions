uniform float uGlow;varying vec3 vCol;varying float vA;varying float vW;varying float vS;varying vec2 vFlow;
void main(){float a=vA*exp(-vW*vW*2.6);if(vFlow.y>0.){float f=.5+.5*sin((vS*vFlow.y-vFlow.x)*6.2831853);a*=.3+.7*f*f;}
  a*=uGlow;gl_FragColor=vec4(vCol*a,a);}