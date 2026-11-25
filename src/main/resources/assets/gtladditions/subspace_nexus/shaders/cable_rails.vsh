attribute vec2 cuv;uniform vec2 uAxis;uniform float uZa;uniform float uZb;uniform float uW;uniform float uMinPx;uniform vec3 uCam;uniform float uPix;
varying float vZ;varying float vS;varying float vCov;varying vec2 vSide;varying vec2 vView;varying vec3 vP;varying float vPx;
void main(){float z=mix(uZa,uZb,cuv.x);vec3 C=vec3(uAxis,z);vec3 V=uCam-C;float lh=length(V.xy);vec2 d=lh>1e-4?V.xy/lh:vec2(1.,0.);
  vec2 side=vec2(-d.y,d.x);float dist=length(V),we=max(uW,.5*uMinPx*uPix*dist);vCov=uW/we;vPx=2.*uW/max(uPix*dist,1e-5);vS=cuv.y;vZ=z;vSide=side;vView=d;
  vP=C+vec3(side*cuv.y*we,0.);gl_Position=projectionMatrix*viewMatrix*vec4(vP,1.);}