uniform float uGlow;uniform float uNeg;uniform vec3 uGold;varying vec4 vCol;varying float vSeed;varying vec2 vLd;
void main(){vec2 d=gl_PointCoord*2.-1.;float r=length(d),th=r>1e-6?atan(d.y,d.x):0.;
  float rad=.66+.13*sin(3.*th+vSeed*6.)+.08*sin(5.*th+vSeed*11.)+.05*sin(7.*th+vSeed*17.);if(r>rad)discard;
  float lit=.12+.88*smoothstep(-.55,.9,dot(d/max(rad,.01),vLd));vec3 c=vCol.rgb*lit*(1.+.5*smoothstep(.55,.95,dot(d,vLd)));c=mix(c,vec3(.02)+uGold*.5*smoothstep(.5,.95,r/rad),uNeg);
  float a=vCol.a*(1.0 - smoothstep(rad-.12,rad,r));gl_FragColor=vec4(c*a*uGlow,a);}