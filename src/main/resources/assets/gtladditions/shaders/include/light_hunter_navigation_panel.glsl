// The chart plane crosses the 3 x 3 passage; keep the display inside that aperture.
#moj_import <gtladditions:light_hunter_navigation_region.glsl>
bool navigationPanelRegion() {
    return lightHunterNavigationPanelRegion(localPosition);
}

float navFwidth(vec2 grad, vec2 dpdx, vec2 dpdy) {
    return abs(dot(grad, dpdx)) + abs(dot(grad, dpdy));
}

float navStroke(float dist, float width, float fw) {
    return 1.0 - smoothstep(width, width + max(fw * 0.8, 0.0012), abs(dist));
}

float navBoxStroke(vec2 p, vec2 center, vec2 halfSize, float width, float fwx, float fwy) {
    vec2 q = abs(p - center) - halfSize;
    return navStroke(max(q.x, q.y), width, q.x > q.y ? fwx : fwy);
}

float navSegment(vec2 p, vec2 a, vec2 b, float width, vec2 dpdx, vec2 dpdy) {
    vec2 delta = b - a;
    float deltaLen2 = dot(delta, delta);
    vec2 offset = p - a - delta * clamp(dot(p - a, delta) / max(deltaLen2, 0.00001), 0.0, 1.0);
    float dist = length(offset);
    vec2 grad = dist > 1e-6 ? offset / dist
        : (deltaLen2 > 1e-10 ? vec2(-delta.y, delta.x) * inversesqrt(deltaLen2) : vec2(1.0, 0.0));
    return navStroke(dist, width, navFwidth(grad, dpdx, dpdy));
}

// Five-column glyph order: N A V S C L I K.
const int NAV_GLYPHS[56] = int[56](
    17,25,21,19,17,17,17, 14,17,17,31,17,17,17,
    17,17,17,17,17,10,4, 15,16,16,14,1,1,30,
    14,17,16,16,16,17,14, 16,16,16,16,16,16,31,
    14,4,4,4,4,4,14, 17,18,20,24,20,18,17
);

float navText(vec2 p, vec2 origin, float size, ivec4 glyphs, int count) {
    ivec2 cell = ivec2(floor(vec2(p.x - origin.x, origin.y - p.y) / size));
    if (cell.x < 0 || cell.x >= count * 6 || cell.y < 0 || cell.y >= 7) return 0.0;
    int column = cell.x % 6;
    if (column >= 5) return 0.0;
    int glyph = glyphs[cell.x / 6];
    return float((NAV_GLYPHS[glyph * 7 + cell.y] >> (4 - column)) & 1);
}

vec4 navigationPanel(vec2 panelDx, vec2 panelDy) {
    vec2 p = localPosition.zy;
    vec2 dpdx = panelDx;
    vec2 dpdy = panelDy;
    if (gl_FrontFacing) {
        p.x = -p.x;
        dpdx.x = -dpdx.x;
        dpdy.x = -dpdy.x;
    }
    float fwx = abs(dpdx.x) + abs(dpdy.x);
    float fwy = abs(dpdx.y) + abs(dpdy.y);
    float crop = 1.0 - smoothstep(1.43, 1.49, max(abs(p.x), abs(p.y)));
    float wire = navBoxStroke(p, vec2(0.0), vec2(1.29, 1.27), 0.008, fwx, fwy);
    wire = max(wire, navSegment(p, vec2(-1.20, 0.94), vec2(1.20, 0.94), 0.004, dpdx, dpdy) * 0.65);
    wire = max(wire, navSegment(p, vec2(0.49, 0.83), vec2(0.49, -0.76), 0.004, dpdx, dpdy) * 0.45);
    for (int side = -1; side <= 1; side += 2) {
        for (int vertical = -1; vertical <= 1; vertical += 2) {
            vec2 corner = vec2(float(side) * 1.34, float(vertical) * 1.32);
            wire = max(wire, navSegment(p, corner, corner - vec2(float(side) * 0.19, 0.0), 0.016, dpdx, dpdy));
            wire = max(wire, navSegment(p, corner, corner - vec2(0.0, float(vertical) * 0.19), 0.016, dpdx, dpdy));
        }
    }

    vec2 q = p - vec2(-0.36, 0.12);
    float radius = length(q);
    vec2 qDir = radius > 1e-5 ? q / radius : vec2(1.0, 0.0);
    float fwRadius = navFwidth(qDir, dpdx, dpdy);
    float radar = max(navStroke(radius - 0.66, 0.005, fwRadius), navStroke(radius - 0.42, 0.004, fwRadius) * 0.5);
    float angle = radius > 1e-5 ? atan(q.y, q.x) : 0.0;
    radar = max(radar, navStroke(radius - 0.71, 0.011, fwRadius) * step(0.28, fract(angle / TAU * 12.0 + 0.5)));
    radar = max(radar, max(navStroke(q.x, 0.002, fwx), navStroke(q.y, 0.002, fwy)) * 0.20 * (1.0 - smoothstep(0.63, 0.66, radius)));
    vec2 grid = abs(fract(q / 0.22 + 0.5) - 0.5) * 0.22;
    radar = max(radar, navStroke(min(grid.x, grid.y), 0.0013, grid.x < grid.y ? fwx : fwy) * 0.11 * (1.0 - smoothstep(0.62, 0.65, radius)));
    vec2 scan = vec2(cos(AngularTime * 0.38), sin(AngularTime * 0.38));
    float sweep = navStroke(q.x * scan.y - q.y * scan.x, 0.006, navFwidth(vec2(scan.y, -scan.x), dpdx, dpdy));
    sweep *= step(0.0, dot(q, scan)) * (1.0 - smoothstep(0.61, 0.66, radius));
    radar = max(radar, sweep * 0.68);
    radar = max(radar, navSegment(q, vec2(-0.43, -0.26), vec2(-0.12, -0.08), 0.009, dpdx, dpdy));
    radar = max(radar, navSegment(q, vec2(-0.12, -0.08), vec2(0.10, 0.20), 0.009, dpdx, dpdy));
    radar = max(radar, navSegment(q, vec2(0.10, 0.20), vec2(0.36, 0.33), 0.009, dpdx, dpdy));
    vec2 targetVec = q - vec2(0.36, 0.33);
    float targetDist = length(targetVec);
    vec2 targetDir = targetDist > 1e-5 ? targetVec / targetDist : vec2(1.0, 0.0);
    float target = navStroke(targetDist - 0.055, 0.011, navFwidth(targetDir, dpdx, dpdy));
    target = max(target, (1.0 - smoothstep(0.012, 0.024, targetDist)) * 0.9);
    wire = max(wire, radar);

    float barReach = 0.011 + max((fwx + fwy) * 0.8, 0.0012) + 1e-4;
    bool inBarX = abs(p.x - 0.8575) < 0.1775 + barReach;
    for (int row = 0; row < 3; ++row) {
        vec2 center = vec2(0.86, 0.55 - float(row) * 0.43);
        wire = max(wire, navBoxStroke(p, center, vec2(0.25, 0.14), 0.004, fwx, fwy) * 0.48);
        if (inBarX && abs(p.y - (center.y - 0.015)) < 0.070 + barReach) {
            for (int bar = 0; bar < 6; ++bar) {
                vec2 b = center + vec2(-0.18 + float(bar) * 0.071, -0.015);
                float h = 0.035 + 0.035 * sin(float(bar * 3 + row) + AngularTime * 0.6);
                wire = max(wire, navSegment(p, b - vec2(0.0, h), b + vec2(0.0, h), 0.011, dpdx, dpdy) * 0.55);
            }
        }
    }
    for (int tab = 0; tab < 3; ++tab) {
        vec2 center = vec2(-0.81 + float(tab) * 0.81, -1.03);
        wire = max(wire, navBoxStroke(p, center, vec2(0.34, 0.12), 0.005, fwx, fwy) * (tab == 0 ? 0.85 : 0.40));
        wire = max(wire, navSegment(p, center - vec2(0.16, 0.01), center + vec2(0.16, -0.01), 0.005, dpdx, dpdy) * 0.55);
    }
    float text = navText(p, vec2(-1.14, 1.18), 0.024, ivec4(0,1,2,0), 3);
    text = max(text, navText(p, vec2(0.62, 0.85), 0.016, ivec4(3,4,1,0), 4));
    text = max(text, navText(p, vec2(-1.12, -0.76), 0.015, ivec4(5,6,0,7), 4));
    float strength = clamp(max(wire, max(text, target)), 0.0, 1.0);
    vec3 color = mix(vec3(0.025, 0.10, 0.19), vec3(0.28, 0.83, 0.98), clamp(wire, 0.0, 1.0));
    color = mix(color, vec3(0.85, 0.96, 1.0), text);
    color = mix(color, vec3(1.0, 0.70, 0.24), target);
    return vec4(color, (0.26 + 0.64 * strength) * crop * tint.a);
}
