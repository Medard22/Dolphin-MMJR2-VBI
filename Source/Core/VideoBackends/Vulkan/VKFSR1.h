// Copyright 2024 Dolphin Emulator Project
// SPDX-License-Identifier: GPL-2.0-or-later

#pragma once

#include <memory>
#include <vector>

#include "Common/CommonTypes.h"
#include "VideoCommon/AbstractTexture.h"

namespace Vulkan
{
class VKTexture;
class VKShader;
class VKPipeline;

// FidelityFX Super Resolution 1.0 implementation for Vulkan backend
// Provides Edge-Adaptive Spatial Upsampling (EASU) and Robust Contrast-Adaptive Sharpening (RCAS)
class FSR1
{
public:
  FSR1();
  ~FSR1();

  // Initialize FSR1 resources (shaders, pipelines, etc.)
  bool Initialize();

  // Apply FSR1 upscaling from source to destination
  // source: Input texture at internal resolution
  // dest: Output texture at target resolution (swapchain size)
  // sharpness: RCAS sharpness value (0.0 = max sharpness, 2.0 = no sharpening)
  void ApplyFSR1(VKTexture* source, VKTexture* dest, float sharpness);

  // Check if FSR1 is supported on current hardware
  static bool IsSupported();

private:
  bool CompileShaders();
  bool CreatePipelines();
  bool CreateDescriptorSetLayouts();
  
  void ApplyEASU(VKTexture* source, VKTexture* intermediate);
  void ApplyRCAS(VKTexture* intermediate, VKTexture* dest, float sharpness);

  // Shader modules
  std::unique_ptr<VKShader> m_easu_shader;
  std::unique_ptr<VKShader> m_rcas_shader;

  // Compute pipelines
  VkPipeline m_easu_pipeline = VK_NULL_HANDLE;
  VkPipeline m_rcas_pipeline = VK_NULL_HANDLE;
  VkPipelineLayout m_easu_pipeline_layout = VK_NULL_HANDLE;
  VkPipelineLayout m_rcas_pipeline_layout = VK_NULL_HANDLE;

  // Descriptor set layouts
  VkDescriptorSetLayout m_easu_descriptor_layout = VK_NULL_HANDLE;
  VkDescriptorSetLayout m_rcas_descriptor_layout = VK_NULL_HANDLE;

  // Descriptor pool for FSR1
  VkDescriptorPool m_descriptor_pool = VK_NULL_HANDLE;

  // Intermediate texture for EASU output / RCAS input
  std::unique_ptr<AbstractTexture> m_intermediate_texture;
  
  bool m_initialized = false;
};

}  // namespace Vulkan
