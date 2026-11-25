uniform float uGlow;varying vec4 vCol;void main(){vec2 d=gl_PointCoord*2.-1.;float r2=dot(d,d);if(r2>1.)discard;
  float a=vCol.a*(exp(-r2*4.)*.85+.15*(1.-r2))*uGlow;gl_FragColor=vec4(vCol.rgb*a,a);}