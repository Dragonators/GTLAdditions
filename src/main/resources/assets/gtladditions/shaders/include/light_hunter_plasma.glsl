uniform float PlasmaPhase[10];

// Each atlas tile stores neighbouring Z slices in R/G.
float plasmaNoise(vec3 p) {
    vec3 cell = mod(floor(p), 64.0);
    vec3 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    vec2 tile = vec2(mod(cell.z, 8.0), floor(cell.z / 8.0));
    vec2 address = (tile * 65.0 + cell.xy + f.xy + 0.5) / 520.0;
    vec2 slices = textureLod(PlasmaNoise, address, 0.0).rg;
    return mix(slices.r, slices.g, f.z);
}

float plasmaFbm(vec3 p, int octave) {
    return plasmaNoise(p + vec3(PlasmaPhase[octave], 0, 0)) * 0.57
        + plasmaNoise(p * 2.07 + 11.3 + vec3(PlasmaPhase[octave + 1], 0, 0)) * 0.29
        + plasmaNoise(p * 4.19 + 23.7 + vec3(PlasmaPhase[octave + 2], 0, 0)) * 0.14;
}

// Keep equal to LightHunterVfxGeometry.MAIN_LENGTH.
const float PLASMA_MAIN_LENGTH = 136.0;

float plasmaOuterRadius(float x, bool mainJet) {
    float u = x / PLASMA_MAIN_LENGTH;
    float radiusIncrease = mainJet ? 0.75 : 0.0;
    float tailSpread = mainJet ? 0.5 * smoothstep(0.5, 1.0, u) : 0.0;
    return 2.48 + radiusIncrease + 4.4 * (1.0 - exp(-x / 26.0)) + u * 1.8 + tailSpread;
}

float plasmaFieldExtent(float x) {
    return 1.1 * plasmaOuterRadius(x, true);
}

vec3 plasmaColor(float x) {
    vec3 throat=vec3(0.30,0.86,1.62);
    vec3 hot=vec3(0.98,1.30,1.72);
    vec3 body=vec3(0.16,0.62,1.78);
    vec3 tail=vec3(0.56,0.28,1.70);

    vec3 c=mix(throat,hot,smoothstep(3.0,11.0,x));
    c=mix(c,body,smoothstep(12.0,30.0,x));
    return mix(c,tail,smoothstep(34.0,70.0,x));
}

float plasmaShockNodes(float x) {
    float nodes=exp(-pow((x-16.0)/2.3,2.0))+0.80*exp(-pow((x-29.2)/2.6,2.0))+0.60*exp(-pow((x-46.25)/3.0,2.0));
    return 1.0+0.75*nodes;
}

vec4 plasmaSample(vec3 q,float len,bool mainJet) {
    float u=q.x/len;
    if(u<0.0||u>1.0)return vec4(0.0);
    float scale=mainJet?1.0:0.38;
    float x=u*PLASMA_MAIN_LENGTH;
    vec2 yz=q.yz/scale;
    float r=length(yz);
    float radiusIncrease=mainJet?0.75:0.0;
    float outerR=plasmaOuterRadius(x,mainJet);
    float radial=r/outerR;
    if(radial>1.1)return vec4(0.0);

    vec3 broad=vec3(x*0.055+PlasmaPhase[0],yz*0.48);
    vec2 bend=vec2(plasmaNoise(broad+11.7),plasmaNoise(broad+31.9))-0.5;
    vec2 curled=yz+bend*(0.85+u*2.20);
    vec3 fibers=vec3(x*0.090,curled*1.25);
    float n=plasmaFbm(fibers,1);
    float wide=plasmaFbm(vec3(x*0.065,curled*0.48)+7.4,4);
    float fine=plasmaNoise(fibers*2.25+vec3(PlasmaPhase[7],12.5,4.0));
    float threads=pow(smoothstep(0.32,0.72,n),1.55);
    float striation=0.30+0.70*smoothstep(0.24,0.76,fine);
    float streamSeed=plasmaNoise(vec3(yz*0.53,PlasmaPhase[8]));
    float packet=plasmaNoise(vec3(x*0.18+PlasmaPhase[9],yz*0.62)+31.7);

    float coreEnd=29.0+streamSeed*21.0;
    float coreR=2.35+radiusIncrease+1.05*(1.0-exp(-x/8.0));
    float coreEdge=length(curled)/coreR+(wide-0.5)*0.30;
    float coreCross=1.0-smoothstep(0.46,1.12,coreEdge);
    float coreLife=1.0-smoothstep(coreEnd-19.0,coreEnd,x);
    float core=coreCross*coreLife*(0.14+threads*1.65)*striation*(0.56+packet*0.80)*plasmaShockNodes(x);

    float tailEnd=PLASMA_MAIN_LENGTH-10.0+10.0*smoothstep(0.20,0.80,streamSeed);
    float tailLife=(0.95-u*0.42)*(1.0-smoothstep(tailEnd-76.0,tailEnd,x));
    float edge=1.0-smoothstep(0.54,1.08,radial+(wide-0.5)*0.26);
    float softGas=exp(-radial*radial*2.8)*(0.08+wide*0.32);
    float flow=exp(-radial*radial*2.0)*threads*1.35*striation;
    float sheath=(softGas+flow)*edge*tailLife;
    float density=(core*1.05+sheath*0.37)/scale;

    vec3 axisColor=plasmaColor(x);
    vec3 rim=vec3(0.10,0.34,1.30);
    rim=mix(rim,vec3(0.22,0.26,1.40),smoothstep(19.0,45.0,x));
    rim=mix(rim,vec3(0.44,0.14,1.18),smoothstep(40.0,69.0,x));

    float bodyHeat=core/(core+sheath*0.55+0.0001);
    bodyHeat*=mix(0.28,1.0,smoothstep(0.10,0.80,coreCross));
    vec3 color=mix(rim,axisColor,bodyHeat*0.88+0.12);
    color*=0.78+threads*0.92;
    return vec4(color,density);
}
