attribute vec2 quv;attribute vec3 iA;attribute vec3 iB;attribute vec4 iW;attribute vec3 iCol;attribute vec2 iAl;
uniform vec3 uCam;uniform float uPix;varying vec3 vCol;varying float vA;varying float vW;varying float vS;varying vec2 vFlow;
void main(){vec3 P=mix(iA,iB,quv.x);vec3 ax=iB-iA;float L=length(ax);vec3 axn=ax/max(L,1e-5);vec3 V=normalize(uCam-P);
  vec3 side=cross(axn,V);float sl=length(side);side=sl>1e-4?side/sl:vec3(1.,0.,0.);
  float w=mix(iW.x,iW.y,quv.x);float we=max(w,1.3*uPix*length(uCam-P));P+=side*quv.y*we*.5;
  vA=mix(iAl.x,iAl.y,quv.x)*min(1.,w/we);vCol=iCol;vW=quv.y;vS=quv.x*L;vFlow=iW.zw;
  gl_Position=projectionMatrix*viewMatrix*vec4(P,1.);}