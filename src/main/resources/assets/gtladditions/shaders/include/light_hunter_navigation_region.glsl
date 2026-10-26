// Shared aperture ownership: the interior NAV display takes priority over exterior star-chart effects.
bool lightHunterNavigationPanelRegion(vec3 position) {
    return abs(position.x + 18.0) < 0.01 && max(abs(position.y), abs(position.z)) < 1.5;
}
