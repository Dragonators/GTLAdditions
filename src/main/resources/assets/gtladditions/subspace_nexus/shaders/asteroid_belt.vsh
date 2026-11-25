attribute float aSize;attribute vec4 aCol;attribute float aSeed;uniform float uScale;uniform vec3 uStarC;varying vec4 vCol;varying float vSeed;varying vec2 vLd;
void main(){vec4 mv=modelViewMatrix*vec4(position,1.);gl_Position=projectionMatrix*mv;float s=aSize*uScale/max(.1,-mv.z);vCol=aCol;vSeed=aSeed;
  vec4 sc=projectionMatrix*viewMatrix*vec4(uStarC,1.);vec2 d=sc.xy/sc.w-gl_Position.xy/gl_Position.w;vLd=normalize(vec2(d.x,-d.y)+1e-5);
  if(s<2.5){vCol.a*=s/2.5;s=2.5;}gl_PointSize=min(s,600.);}