attribute float aTh;attribute float aSide;uniform vec3 uCam;uniform float uPix;uniform vec3 uC;uniform vec3 uU;uniform vec3 uV;uniform float uR;uniform float uHaloW;
varying float vTh;varying float vS;varying float vW;varying float vPx;varying float vD;
void main(){float th=aTh*6.2831853;vec3 rd=cos(th)*uU+sin(th)*uV,T=-sin(th)*uU+cos(th)*uV,P=uC+uR*rd,V=uCam-P;float dist=length(V);V/=dist;
  vec3 s=cross(T,V);float l=length(s);s=l>1e-4?s/l:normalize(cross(uU,uV));
  float px=uPix*dist,w=max(uHaloW,4.*px),dc=length(uCam-uC);vTh=aTh;vS=aSide;vW=w;vPx=px;vD=clamp((dist-max(0.,dc-uR))/(2.*min(uR,dc)+1e-3),0.,1.);
  gl_Position=projectionMatrix*viewMatrix*vec4(P+s*aSide*w,1.);}