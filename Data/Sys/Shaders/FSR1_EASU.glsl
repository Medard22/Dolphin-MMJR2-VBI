// FidelityFX Super Resolution 1.0 - EASU (Edge-Adaptive Spatial Upsampling) Shader
// AMD FidelityFX Super Resolution
// Copyright (c) 2021 Advanced Micro Devices, Inc. All rights reserved.
// SPDX-License-Identifier: MIT
// Simplified GLSL implementation for Dolphin

#version 450 core
#extension GL_GOOGLE_include_directive : require

layout(local_size_x = 16, local_size_y = 16) in;

// Input texture (internal resolution)
layout(binding = 0) uniform sampler2D inputTexture;

// Output texture (upscaled to target resolution)
layout(binding = 1, rgba8) uniform writeonly image2D outputImage;

// Constants for EASU
layout(std140, binding = 2) uniform EASUConstants
{
  vec4 Const0;  // Input viewport dimensions, output size
  vec4 Const1;  // Scaling ratios
  vec4 Const2;  // Additional parameters
  vec4 Const3;  // Additional parameters
} easuConst;

// FSR1 EASU filter (simplified Lanczos-like reconstruction)
vec4 FsrEasuF(vec2 pos)
{
  vec2 inputSize = textureSize(inputTexture, 0);
  vec2 outputSize = imageSize(outputImage);
  vec2 scale = inputSize / outputSize;
  
  // Map output position to input space
  vec2 inputPos = (pos + 0.5) * scale;
  
  // Sample positions for bicubic-like reconstruction
  vec2 centerPos = inputPos - 0.5;
  vec2 f = fract(centerPos);
  centerPos = floor(centerPos) + 0.5;
  
  // Catmull-Rom / Bicubic weights
  vec2 f2 = f * f;
  vec2 f3 = f2 * f;
  
  vec2 w0 = -0.5 * f3 + f2 - 0.5 * f;
  vec2 w1 = 1.5 * f3 - 2.5 * f2 + 1.0;
  vec2 w2 = -1.5 * f3 + 2.0 * f2 + 0.5 * f;
  vec2 w3 = 0.5 * f3 - 0.5 * f2;
  
  vec2 w12 = w1 + w2;
  vec2 tc0 = (centerPos - 1.0) / inputSize;
  vec2 tc12 = (centerPos + w2 / w12) / inputSize;
  vec2 tc3 = (centerPos + 2.0) / inputSize;
  
  vec4 color = vec4(0.0);
  color += texture(inputTexture, vec2(tc0.x, tc0.y)) * w0.x * w0.y;
  color += texture(inputTexture, vec2(tc12.x, tc0.y)) * w12.x * w0.y;
  color += texture(inputTexture, vec2(tc3.x, tc0.y)) * w3.x * w0.y;
  
  color += texture(inputTexture, vec2(tc0.x, tc12.y)) * w0.x * w12.y;
  color += texture(inputTexture, vec2(tc12.x, tc12.y)) * w12.x * w12.y;
  color += texture(inputTexture, vec2(tc3.x, tc12.y)) * w3.x * w12.y;
  
  color += texture(inputTexture, vec2(tc0.x, tc3.y)) * w0.x * w3.y;
  color += texture(inputTexture, vec2(tc12.x, tc3.y)) * w12.x * w3.y;
  color += texture(inputTexture, vec2(tc3.x, tc3.y)) * w3.x * w3.y;
  
  return color;
}

void main()
{
  ivec2 gid = ivec2(gl_GlobalInvocationID.xy);
  ivec2 outputSize = imageSize(outputImage);
  
  if (gid.x >= outputSize.x || gid.y >= outputSize.y)
    return;
  
  vec4 color = FsrEasuF(vec2(gid));
  imageStore(outputImage, gid, color);
}
