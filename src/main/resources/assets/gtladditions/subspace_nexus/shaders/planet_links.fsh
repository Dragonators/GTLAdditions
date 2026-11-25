uniform vec3 uCol;uniform float uA;uniform float uCoreW;uniform float uHaloW;uniform float uHaloA;uniform float uBeat;uniform float uEnd;uniform float uBeatPhase;uniform float uGlow;
varying float vLen;varying float vTo;varying vec2 vR;varying float vS;varying float vW;varying float vPx;
void main(){float y=abs(vS)*vW,cw=max(uCoreW,.55*vPx),core=(1.-smoothstep(cw*.35,cw+.6*vPx,y))*min(1.,uCoreW/cw);
  float hw=max(uHaloW*.5,1.6*vPx),halo=exp(-y*y/(hw*hw))*min(1.,uHaloW*.5/hw)*(1.-smoothstep(.75,1.,abs(vS)));
  float a=(core+uHaloA*halo)*(1.-uBeat*(.5+.5*cos((vLen*.05-uBeatPhase)*6.2831853)))*smoothstep(vR.x,vR.x+uEnd,vLen)*smoothstep(vR.y,vR.y+uEnd,vTo)*uA*uGlow;   // eases in just above each surface, so a link aimed at the viewer is no spike on the planet's face
  gl_FragColor=vec4(mix(uCol,vec3(1.),.3*core)*a,a);}