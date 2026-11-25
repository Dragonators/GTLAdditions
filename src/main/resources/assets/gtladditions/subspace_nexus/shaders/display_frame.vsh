attribute float aAng;attribute float aSide;uniform vec3 uCam;uniform float uPix;uniform vec3 uGeo;uniform float uBand;uniform float uTopOnly;uniform float uUp;varying float vV;varying float vAng;varying float vK;
void main(){float c=cos(aAng),s=sin(aAng);vec3 C=vec3(uGeo.x*c,uGeo.x*s,uGeo.y),T=vec3(-s,c,0.),V=normalize(uCam-C),S=cross(T,V);float l=length(S);S=l>1e-4?S/l:vec3(0.,0.,1.);
  float w=uGeo.z;if(uBand>.5){S=vec3(0.,0.,1.);vK=1.;}else{float we=max(w,1.4*uPix*length(uCam-C));vK=min(1.,w/we);w=we;}
  if(uTopOnly>.5)vK*=smoothstep(.72,.92,abs(V.z));
  if(uBand>.5)C.z+=w*uUp;
  vV=aSide;vAng=aAng;gl_Position=projectionMatrix*viewMatrix*vec4(C+S*aSide*w,1.);}