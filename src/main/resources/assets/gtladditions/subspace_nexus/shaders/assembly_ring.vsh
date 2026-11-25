attribute vec4 aSec;uniform float uR;uniform float uGlitch;uniform vec4 uRingPhase;uniform float uSeed;
varying vec3 vP;varying vec3 vN;varying vec3 vL;varying float vU;varying float vRole;varying float vZ;varying float vRo;varying float vRad;varying vec3 vT;
float hh(float n){return fract(sin(n*127.1+.3)*43758.5453);}
void main(){vU=uv.x;vRo=aSec.x;vRole=aSec.y;vZ=aSec.z;vRad=uR+aSec.x;vL=position;vT=normalize(mat3(modelMatrix)*vec3(-position.y,position.x,0.));   // vT is the world-space tangent along the ring.
  vec3 p=position;float tg=uRingPhase.x,sl=floor(uv.x*64.),jit=uGlitch>.001?step(hh(sl+tg*1.3+uSeed),.22*uGlitch):0.;   // step(h, 0.) also fires when the hash is exactly 0 (~1/400), so guard against glitches while disabled.
  p+=normalize(vec3(p.xy,0.)+vec3(1e-4,0.,0.))*jit*(hh(sl*1.7+tg)-.35)*1.6;p.z+=jit*(hh(sl*2.3+tg)-.5)*1.2;
  vec4 wp=modelMatrix*vec4(p,1.);vP=wp.xyz;vN=normalize(mat3(modelMatrix)*normal);gl_Position=projectionMatrix*viewMatrix*wp;}