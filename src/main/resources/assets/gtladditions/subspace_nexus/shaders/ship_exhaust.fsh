uniform vec3 uCam;uniform vec3 uColor;uniform float uAlpha;uniform vec2 uDash;uniform float uGlow;uniform float uHead;varying vec3 vP;varying vec3 vN;varying float vH;
void main(){float f=abs(dot(normalize(vN),normalize(uCam-vP)));float a=uAlpha*pow(f,1.6)*mix(1.,pow(max(0.,1.-vH),1.5),uHead);
  if(uDash.y>0.){float d=.5+.5*sin((vP.z*uDash.y-uDash.x)*6.2831853);a*=.45+.55*d;}
  a*=uGlow;gl_FragColor=vec4(uColor*a,a);}