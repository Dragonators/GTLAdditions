attribute vec2 ruv;attribute vec3 iC;attribute vec3 iN;attribute vec4 iP;attribute vec3 iCol;attribute vec4 iS;attribute vec2 iArc;
uniform vec3 uCam;uniform float uPix;
varying float vA;varying vec3 vCol;varying float vU;varying vec4 vS;varying float vF;varying float vRot;varying vec2 vArc;
void main(){
  vec3 n=normalize(iN);vec3 e1;vec3 e2;
  if(abs(n.z)>.999){e1=vec3(1.,0.,0.);e2=vec3(0.,1.,0.)*sign(n.z);}else{e1=normalize(cross(n,vec3(0.,0.,1.)));e2=cross(n,e1);}
  float th=ruv.x*6.2831853;vec3 rd=cos(th)*e1+sin(th)*e2;vec3 P=iC+iP.x*rd;
  float dist=length(uCam-P);float tube=max(iP.y,.55*uPix*dist);
  float ph=ruv.y*6.2831853;vec3 nr=cos(ph)*rd+sin(ph)*n;vec3 pos=P+tube*nr;
  vA=iP.z*min(1.,iP.y/tube);vF=abs(dot(nr,normalize(uCam-pos)));
  vCol=iCol;vU=ruv.x;vS=iS;vRot=iP.w;vArc=iArc;
  gl_Position=projectionMatrix*viewMatrix*vec4(pos,1.);
}