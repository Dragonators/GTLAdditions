#undef main
uniform sampler2D NexusHandMask;
uniform bool NexusHandMaskEnabled;

void main() {
    // The material first: its derivatives are undefined after a discard.
    nexusMaterialMain();
    if (NexusHandMaskEnabled && texelFetch(NexusHandMask, ivec2(gl_FragCoord.xy), 0).r > 0.5) discard;
}
