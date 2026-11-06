#version 330
#extension GL_ARB_separate_shader_objects : require

// Retro Eras: the lighting of Cave game and Classic, used in place of 26.3's lightmap while you are in those eras. They had
// no light levels at all. A block was lit if nothing stood above it, up to the sky, and otherwise in shadow, at one fixed
// shade: Level.getBrightness gave 0.8 in rd-132211 and 0.6 in Classic. The mod fills in SHADOW for each.

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

void main() {
    float block_level = floor(texCoord.x * 16);
    float sky_level = floor(texCoord.y * 16);

    vec3 color = vec3(sky_level >= 15.0 || block_level >= 15.0 ? 1.0 : SHADOW);
    color = max(color, lightmapInfo.NightVisionColor * lightmapInfo.NightVisionFactor);
    color = color - vec3(lightmapInfo.DarknessScale);

    fragColor = vec4(clamp(color, 0.0, 1.0), 1.0);
}
