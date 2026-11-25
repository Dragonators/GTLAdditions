uniform float AngularTime;
uniform vec2 ShieldOffset;
uniform int Effect;
in vec2 uv;
in vec4 tint;
in vec3 localPosition;
flat in vec3 localEye;
flat in vec3 localForward;
flat in float orthoCamera;
vec4 materialColor;
const float TAU = 6.28318530718;
const int SHIELD_FIELD_EFFECT = 7;
const float SHIELD_CELL_SIZE = 3.2;
const float SHIELD_LINE_WIDTH = 0.045;
const float SHIELD_CORE_WIDTH = 0.012;
const float SHIELD_CHARGE_SPEED = 4.0;
const float SHIELD_CHARGE_GAIN = 1.8;
// The geometry's shield tint alpha is 0.66; retain its control over all shield layers.
const float SHIELD_TINT_ALPHA = 0.66;
float shieldHash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}
float shieldLatticeHash(vec2 cell) {
    return shieldHash(mod(cell + 512.0, 1024.0) - 512.0);
}
float shieldNoise(vec2 p) {
    vec2 cell = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(shieldLatticeHash(cell), shieldLatticeHash(cell + vec2(1.0, 0.0)), f.x),
        mix(shieldLatticeHash(cell + vec2(0.0, 1.0)), shieldLatticeHash(cell + vec2(1.0, 1.0)), f.x), f.y);
}
// Local hex coordinates in xy, quantized lattice centre in zw.
vec4 shieldHexCell(vec2 p) {
    vec2 spacing = vec2(1.7320508, 1.0);
    // Quantized centres keep each cell's phase stable across pixels and triangle boundaries.
    vec2 centreA = floor(p / spacing) + 0.5;
    vec2 centreB = floor((p - spacing * 0.5) / spacing) + 1.0;
    vec2 a = p - centreA * spacing;
    vec2 b = p - centreB * spacing;
    bool first = dot(a, a) < dot(b, b);
    return vec4(first ? a : b, first ? centreA : centreB);
}
// Continuous phase offsets keep neighbouring cells flowing without per-event resets.
vec2 shieldCharge(vec2 id, float time) {
    float seed = shieldHash(id + vec2(19.2, 31.1));
    float clusters = shieldNoise(id * vec2(1.7320508, 1.0) * 0.16 + vec2(13.0, 4.0));
    float t = time * SHIELD_CHARGE_SPEED;
    float groupPulse = pow(0.5 + 0.5 * sin(clusters * 10.0 - t * 0.52), 5.0);
    float cellPhase = 0.5 + 0.5 * sin(t * 0.67 + seed * 2.2 + clusters * 3.0);
    float charge = groupPulse * smoothstep(0.24, 0.66, clusters) * (0.64 + 0.36 * cellPhase);
    return vec2(charge, charge * charge);
}
float shieldFresnel() {
    vec3 normal = cross(dFdx(localPosition), dFdy(localPosition));
    float size = length(normal);
    if (size < 1e-8) return 0.0;
    vec3 view = orthoCamera > 0.5 ? -localForward : localEye - localPosition;
    return pow(1.0 - abs(dot(normal / size, normalize(view))), 3.0);
}
vec4 shieldHoneycomb(vec2 surface, float time, float latent) {
    vec2 p = surface / SHIELD_CELL_SIZE;
    vec4 hex = shieldHexCell(p);
    vec2 h = hex.xy;
    vec2 id = hex.zw;
    float edge = max(0.0, 0.5 - max(abs(h.y), abs(h.x) * 0.8660254 + abs(h.y) * 0.5));
    float antialias = max(fwidth(edge) * 0.8, 0.004);
    float grid = 1.0 - smoothstep(SHIELD_LINE_WIDTH, SHIELD_LINE_WIDTH + antialias, edge);
    float core = 1.0 - smoothstep(SHIELD_CORE_WIDTH, SHIELD_CORE_WIDTH + antialias, edge);
    float halo = exp(-edge * 18.0);
    float clusters = shieldNoise(id * vec2(1.7320508, 1.0) * 0.16 + vec2(13.0, 4.0));
    float region = smoothstep(0.33, 0.78, clusters);
    vec2 chargeLevels = shieldCharge(id, time);
    float charge = chargeLevels.x;
    float grain = 0.86 + 0.14 * shieldNoise(surface * 2.1);
    float lineEnergy = grid * (mix(0.028, 0.085, latent) + region * 0.035 * latent + 0.25 * charge * SHIELD_CHARGE_GAIN) * grain;
    float coreEnergy = core * chargeLevels.y * 0.14 * SHIELD_CHARGE_GAIN;
    float haloEnergy = halo * charge * 0.035 * SHIELD_CHARGE_GAIN;
    float fillEnergy = smoothstep(0.09, 0.27, edge) * charge * 0.026 * SHIELD_CHARGE_GAIN;
    // Fade only subpixel cells, never an entire folded shield face at grazing angles.
    float footprint = max(length(dFdx(p)), length(dFdy(p)));
    float detailFade = 1.0 - smoothstep(0.24, 0.62, footprint);
    float intensity = lineEnergy + coreEnergy + haloEnergy + fillEnergy;
    vec3 color = mix(vec3(0.15, 0.39, 1.0), vec3(0.22, 0.79, 1.0), charge * 0.75);
    color = mix(color, vec3(0.55, 0.94, 1.0), coreEnergy / max(intensity, 0.00001));
    return vec4(color, intensity * detailFade);
}
float edgeDistance(vec2 p, vec2 a, vec2 b) {
    // Side plates are roughly twice as long as they are tall.
    p.x *= 2.0; a.x *= 2.0; b.x *= 2.0;
    vec2 ab = b - a;
    return length(p - a - ab * clamp(dot(p - a, ab) / dot(ab, ab), 0.0, 1.0));
}
float shieldBorder(vec2 p, float shoulder, float notch) {
    // Matches the actual concave polygons in LightHunterVfxGeometry.shieldFlank/shieldTop.
    vec2 outline[6] = vec2[6](vec2(0.0,0.5), vec2(shoulder,0.0), vec2(1.0,0.0),
        vec2(notch,0.5), vec2(1.0,1.0), vec2(shoulder,1.0));
    float d = 10.0;
    for (int i = 0; i < 6; ++i) d = min(d, edgeDistance(p, outline[i], outline[(i + 1) % 6]));
    return d;
}
float tailHash(vec3 p) {
    p = fract(p * 0.1031);
    p += dot(p, p.yzx + 33.33);
    return fract((p.x + p.y) * p.z);
}
float tailNoise(vec3 p) {
    vec3 i = floor(p), f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(mix(tailHash(i), tailHash(i+vec3(1,0,0)), f.x),
                   mix(tailHash(i+vec3(0,1,0)), tailHash(i+vec3(1,1,0)), f.x), f.y),
               mix(mix(tailHash(i+vec3(0,0,1)), tailHash(i+vec3(1,0,1)), f.x),
                   mix(tailHash(i+vec3(0,1,1)), tailHash(i+vec3(1,1,1)), f.x), f.y), f.z);
}
float tailFbm(vec3 p) {
    return tailNoise(p) * 0.57 + tailNoise(p * 2.07 + 11.3) * 0.29
        + tailNoise(p * 4.19 + 23.7) * 0.14;
}
float bell(float x, float width) { return exp(-x*x / (width*width)); }
vec4 refinedRoot() {
    vec2 p = (uv - 0.5) * 2.0;
    float r = length(p);
    float body = exp(-r*r*9.0);
    float lip = bell(r - 0.47, 0.075) * 0.15;
    float halo = exp(-r*r*3.2) * 0.12;
    float cutoff = 1.0 - smoothstep(0.76, 1.0, r);
    vec3 c = mix(vec3(0.25,0.37,1.0), vec3(0.78,0.91,1.0), exp(-r*r*14.0));
    return vec4(c, (body*0.88+lip+halo) * cutoff * tint.a);
}


// Independently authored; not the source material from the commercial Blender asset.
uniform sampler2D SceneDepth;
uniform sampler2D PlasmaNoise;
uniform vec2 SceneViewport;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

#moj_import <gtladditions:light_hunter_plasma_field.glsl>

vec2 plasmaBox(vec3 origin, vec3 direction, float len, float radius) {
    vec3 d=vec3(abs(direction.x)<0.00001?0.00001:direction.x,
                abs(direction.y)<0.00001?0.00001:direction.y,
                abs(direction.z)<0.00001?0.00001:direction.z);
    vec3 a=(vec3(0,-radius,-radius)-origin)/d;
    vec3 b=(vec3(len,radius,radius)-origin)/d;
    vec3 nearP=min(a,b),farP=max(a,b);
    return vec2(max(max(nearP.x,nearP.y),max(nearP.z,0.0)),min(min(farP.x,farP.y),farP.z));
}

#ifndef LIGHT_HUNTER_LAYERED
vec2 effectScreenPixel() { return gl_FragCoord.xy; }
void layerBegin(vec3 eyeView, vec3 dirView) {}
void layerAccumulate(float density, float segment, float distance, float absorption, float transmission, vec3 color) {}
float layerClipSegment(float distance, float segment) { return segment; }
void layerAccumulateEmission(vec3 emission, float segment, float distance) {}
vec4 layerFinish(vec3 light, float exposure) { return vec4(vec3(1.0)-exp(-light*exposure),1.0); }
#endif
#moj_import <gtladditions:light_hunter_ring.glsl>

vec4 auxiliaryVolume(vec3 origin, vec3 direction, vec2 hit) {
    float stepSize = max(0.12, (hit.y - hit.x) / 16.0);
    vec3 light = vec3(0.0);
    float transmission = 1.0;
    for (int i = 0; i < 16; i++) {
        float distance = hit.x + float(i) * stepSize;
        float segment = min(stepSize, hit.y - distance);
        if (segment <= 0.0) break;
        vec3 q = origin + direction * (distance + 0.5 * segment);
        float u = clamp(q.x / 25.0, 0.0, 1.0);
        float radius = max(1.15 * (1.0 - u), 0.012);
        float radial = length(q.yz) / radius;
        if (radial >= 1.0) continue;
        float edge = 1.0 - smoothstep(0.72, 1.0, radial);
        float life = pow(1.0 - u, 1.6) * (1.0 - smoothstep(0.62, 1.0, u));
        float flow = 0.92 + 0.08 * sin(q.x * 1.4 - AngularTime * 8.0);
        float density = exp(-radial * radial * 5.0) * edge * life * flow;
        vec3 color = mix(vec3(0.14, 0.40, 1.30), vec3(0.50, 0.90, 1.42), exp(-u * 4.5));
        color = mix(color, vec3(0.90, 1.04, 1.30), exp(-radial * radial * 13.0) * (1.0 - u) * (1.0 - u) * 0.50);
        float alpha = 1.0 - exp(-density * segment * 2.1);
        layerAccumulate(density,segment,distance,2.1,transmission,color*1.20);
        light += transmission * alpha * color * 1.20;
        transmission *= exp(-density * segment * 0.45);
    }
    return layerFinish(light,1.45);
}


vec4 volumePlume() {
    if(gl_FrontFacing)discard;
    bool mainJet=tint.a>0.33;
    float len=mainJet?PLASMA_MAIN_LENGTH:25.0;
    vec2 centre=mainJet?vec2(0.0):round(localPosition.yz/13.0)*13.0;
    vec3 rd=normalize(orthoCamera>0.5?localForward:localPosition-localEye);
    vec3 eye=orthoCamera>0.5?localPosition-rd*700.0:localEye;
    vec3 ro=vec3(-180.55-eye.x,eye.yz-centre);
    vec3 direction=vec3(-rd.x,rd.yz);
    float surface=dot(localPosition-eye,rd)+0.001;
    float sceneZ=texelFetch(SceneDepth,ivec2(effectScreenPixel()-SceneViewport),0).r*2.0-1.0;
    float viewZ=(ProjMat[3][2]-sceneZ*ProjMat[3][3])/(sceneZ*ProjMat[2][3]-ProjMat[2][2]);
    vec3 eyeView=(ModelViewMat*vec4(eye,1.0)).xyz;
    vec3 dirView=(ModelViewMat*vec4(rd,0.0)).xyz;
    surface=min(surface,(viewZ-eyeView.z)/dirView.z);
    vec2 hit=plasmaBox(ro,direction,len,mainJet?12.25:1.15);
    hit.y=min(hit.y,surface);
    if(hit.y<=hit.x)discard;
    layerBegin(eyeView,dirView);
    if(!mainJet)return auxiliaryVolume(ro,direction,hit);
    // Keep the sampling lattice continuous as an occluder clips a ray. Changing
    // every sample position at integer step-count boundaries creates visible bands.
    float stepSize=max(mainJet?0.4:0.152,(hit.y-hit.x)/64.0);
    vec3 light=vec3(0.0);
    float transmission=1.0;
    for(int i=0;i<64;i++) {
        float distance=hit.x+float(i)*stepSize;
        float segmentSize=min(stepSize,hit.y-distance);
        if(segmentSize<=0.0)break;
        vec3 q=ro+direction*(distance+0.5*segmentSize);
        vec4 sample=samplePlasmaField(q,len,mainJet);
        float alpha=1.0-exp(-sample.a*segmentSize*0.49);
        layerAccumulate(sample.a,segmentSize,distance,0.49,transmission,sample.rgb*1.34);
        light+=transmission*alpha*sample.rgb*1.34;
        transmission*=exp(-sample.a*segmentSize*0.24);
    }
    return layerFinish(light,1.05);
}


#moj_import <gtladditions:light_hunter_navigation_panel.glsl>
#moj_import <gtladditions:light_hunter_window_coverage.glsl>
uniform vec3 ParadoxSignal;
uniform float ParadoxPlane;

bool chartCoveredByWindow() {
    if (ParadoxSignal.x<1.0) return false;
    vec3 direction=orthoCamera>0.5?localForward:localPosition-localEye;
    if (abs(direction.x)<0.00001) return false;
    vec3 point=localPosition+direction*((ParadoxPlane-localPosition.x)/direction.x);
    if (lightHunterNavigationPanelRegion(vec3(-18.0,point.yz))) return false;
    return lightHunterWindowCoverage(lightHunterWindowCoord(point),ParadoxSignal)>=0.999;
}

void materialMain() {
    bool navigation = navigationPanelRegion();
    if (navigation && Effect == 6) {
        materialColor = navigationPanel();
        return;
    }
    if (navigation && (Effect == 3 || Effect == 4)) discard;
    // Coplanar layers must not depend on a sub-depth-buffer-unit geometric offset at long range.
    if ((Effect == 3 || Effect == 4 || Effect == 6) && chartCoveredByWindow()) discard;
    float a = 0.0;
    vec3 color = tint.rgb;
    if (Effect == 0) {
        materialColor = volumePlume();
        return;
    } else if (Effect == 1) {
        materialColor = volumeRing();
        return;
    } else if (Effect == 2 || Effect == SHIELD_FIELD_EFFECT) {
        bool top = uv.x > 5.5;
        vec2 panelUv = vec2(mod(uv.x, 2.0), uv.y);
        float border = shieldBorder(panelUv, top ? 0.28 : (uv.x > 3.5 ? 0.12 : 0.0), top ? 0.76 : 0.80);
        float interior = smoothstep(0.025, 0.08, border) * smoothstep(0.007, 0.028, abs(panelUv.y - 0.5));
        vec2 surface = vec2(localPosition.x, top ? localPosition.z : localPosition.y);
        float rim = 1.0 - smoothstep(0.015, 0.11, border);
        float fresnel = shieldFresnel();
        if (Effect == SHIELD_FIELD_EFFECT) {
            float field = shieldNoise(surface * 0.085 + ShieldOffset);
            color = mix(vec3(0.02, 0.08, 0.38), vec3(0.035, 0.16, 0.62), field);
            float charge = shieldCharge(shieldHexCell(surface / SHIELD_CELL_SIZE).zw, AngularTime).x;
            color = mix(color, vec3(0.06, 0.26, 0.74), charge * 0.16);
            color = mix(color, vec3(0.06, 0.22, 0.86), max(rim, fresnel) * 0.40);
            a = (0.27 + field * 0.08 + rim * 0.16 + fresnel * 0.20) * interior * tint.a / SHIELD_TINT_ALPHA;
            materialColor = vec4(color, clamp(a, 0.0, 1.0));
            return;
        }
        float seam = exp(-border / max(0.01, fwidth(border) * 1.2));
        float glow = exp(-border * 30.0);
        float ridge = exp(-abs(panelUv.y - 0.5) * 90.0);
        float scan = pow(0.5 + 0.5 * sin(panelUv.x * 28.0 - AngularTime * 0.65), 32.0);
        float cells = pow(0.5 + 0.5 * sin(panelUv.x * 87.0 + sin(abs(panelUv.y - 0.5) * 40.0)), 16.0);
        a = mix(0.045, 0.022, interior) + seam * 0.85 + glow * 0.30 + ridge * 0.12;
        a += scan * 0.035 * (1.0 - interior) + cells * 0.02 * (1.0 - interior);
        // Keep both sides visible at grazing angles. Raster coverage and fwidth handle thin edges;
        // fading an entire face by its normal made folded lower shields disappear in wedges.
        color = mix(vec3(0.26, 0.42, 1.0), vec3(0.64, 0.76, 1.0), seam);
        color *= 1.45;
        // Model-space cells share one scale on all plates and stay fixed while charge moves.
        vec4 honeycomb = shieldHoneycomb(surface, AngularTime, max(rim, fresnel * 0.6));
        float faceAlpha = honeycomb.a * interior * tint.a / SHIELD_TINT_ALPHA;
        if (faceAlpha > 0.0) {
            float edgeAlpha = clamp(a * tint.a, 0.0, 1.0);
            float alpha = clamp(edgeAlpha + faceAlpha, 0.0, 1.0);
            // Preserve additive light from both layers with the existing SRC_ALPHA / ONE pass.
            vec3 emission = color * edgeAlpha + honeycomb.rgb * faceAlpha;
            materialColor = vec4(emission / max(alpha, 0.00001), alpha);
            return;
        }
    } else if (Effect == 3) {
        float d = (uv.y - 0.5) * 2.0;
        a = exp(-d * d * 4.0) * 1.65;
        color = mix(color, vec3(0.55, 0.90, 1.0), exp(-d * d * 24.0));
    } else if (Effect == 4) {
        vec2 p = (uv - 0.5) * 2.0;
        float d = dot(p, p);
        a = exp(-d * 16.0) + 0.2 * exp(-d * 3.0);
        a += 0.45 * exp(-abs(p.x * p.y) * 100.0) * exp(-d * 2.5);
        a *= 0.85 + 0.15 * sin(AngularTime * 1.1 + tint.b * 13.0);
        color = mix(color, vec3(0.78, 0.96, 1.0), exp(-d * 30.0)) * 1.8;
    } else if (Effect == 5) {
        materialColor = refinedRoot();
        return;
    } else {
        vec2 p = (uv - 0.5) * 2.0;
        float r = length(p);
        float edge = 1.0 - smoothstep(0.965, 1.0, r);
        float drift = 0.5 + 0.5 * sin(p.x * 3.0 + p.y * 2.0 - AngularTime * 0.16);
        a = (0.28 + 0.04 * drift) * edge;
        color = mix(vec3(0.08, 0.28, 0.78), vec3(0.14, 0.46, 0.96), 0.5 + p.x * 0.5);
    }
    materialColor = vec4(color, clamp(a * tint.a, 0.0, 1.0));
}
