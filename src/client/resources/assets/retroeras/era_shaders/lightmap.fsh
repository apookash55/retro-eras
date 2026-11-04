#version 330
#extension GL_ARB_separate_shader_objects : require

// Retro Eras: the lighting of Infdev, Alpha and Beta, used in place of 26.3's lightmap while you are in those eras.
// Before Beta 1.8 a block was lit by the brighter of its sky light (less the time of day, in whole steps of 0 to 11)
// and its block light, through one grey brightness table: no warm torchlight, no blue nights, no flicker, and no
// adding the two together. The mod passes the stepped daylight in as SkyFactor.

layout(std140) uniform LightmapInfo {
    float SkyFactor;
    float BlockFactor;
    float NightVisionFactor;
    float DarknessScale;
    float BossOverlayWorldDarkeningFactor;
    float BrightnessFactor;
    vec3 BlockLightTint;
    vec3 SkyLightColor;
    vec3 AmbientColor;
    vec3 NightVisionColor;
} lightmapInfo;

layout(location = 0) in vec2 texCoord;

layout(location = 0) out vec4 fragColor;

// Beta's WorldProvider.generateLightBrightnessTable, with its 0.05 floor.
float old_brightness(float level) {
    float f = 1.0 - level / 15.0;
    return (1.0 - f) / (f * 3.0 + 1.0) * 0.95 + 0.05;
}

vec3 notGamma(vec3 color) {
    float maxComponent = max(max(color.x, color.y), color.z);
    float maxInverted = 1.0f - maxComponent;
    float maxScaled = 1.0f - maxInverted * maxInverted * maxInverted * maxInverted;
    return color * (maxScaled / maxComponent);
}

void main() {
    float block_level = floor(texCoord.x * 16);
    float sky_level = floor(texCoord.y * 16);
    float subtracted = floor((1.0 - clamp(lightmapInfo.SkyFactor, 0.0, 1.0)) * 11.0 + 0.5);
    float level = max(max(sky_level - subtracted, 0.0), block_level);

    vec3 color = vec3(old_brightness(level));
    color = max(color, lightmapInfo.NightVisionColor * lightmapInfo.NightVisionFactor);
    color = mix(color, color * vec3(0.7, 0.6, 0.6), lightmapInfo.BossOverlayWorldDarkeningFactor);
    color = color - vec3(lightmapInfo.DarknessScale);

    // Keep the Brightness option working, as Beta's own brightness setting did.
    color = clamp(color, 0.0, 1.0);
    color = mix(color, notGamma(color), lightmapInfo.BrightnessFactor);

    fragColor = vec4(color, 1.0);
}
