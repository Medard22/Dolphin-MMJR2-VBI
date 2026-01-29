// Copyright 2024 Dolphin Emulator Project
// SPDX-License-Identifier: GPL-2.0-or-later

#include "VideoBackends/Vulkan/VKFSR1.h"

#include "Common/Align.h"
#include "Common/FileUtil.h"
#include "Common/Logging/Log.h"
#include "Common/MsgHandler.h"

#include "VideoBackends/Vulkan/CommandBufferManager.h"
#include "VideoBackends/Vulkan/ObjectCache.h"
#include "VideoBackends/Vulkan/ShaderCompiler.h"
#include "VideoBackends/Vulkan/StateTracker.h"
#include "VideoBackends/Vulkan/VKShader.h"
#include "VideoBackends/Vulkan/VKTexture.h"
#include "VideoBackends/Vulkan/VulkanContext.h"

#include "VideoCommon/VideoConfig.h"

namespace Vulkan
{

FSR1::FSR1() = default;

FSR1::~FSR1()
{
  if (m_easu_pipeline != VK_NULL_HANDLE)
    vkDestroyPipeline(g_vulkan_context->GetDevice(), m_easu_pipeline, nullptr);
  
  if (m_rcas_pipeline != VK_NULL_HANDLE)
    vkDestroyPipeline(g_vulkan_context->GetDevice(), m_rcas_pipeline, nullptr);
  
  if (m_easu_pipeline_layout != VK_NULL_HANDLE)
    vkDestroyPipelineLayout(g_vulkan_context->GetDevice(), m_easu_pipeline_layout, nullptr);
  
  if (m_rcas_pipeline_layout != VK_NULL_HANDLE)
    vkDestroyPipelineLayout(g_vulkan_context->GetDevice(), m_rcas_pipeline_layout, nullptr);
  
  if (m_easu_descriptor_layout != VK_NULL_HANDLE)
    vkDestroyDescriptorSetLayout(g_vulkan_context->GetDevice(), m_easu_descriptor_layout, nullptr);
  
  if (m_rcas_descriptor_layout != VK_NULL_HANDLE)
    vkDestroyDescriptorSetLayout(g_vulkan_context->GetDevice(), m_rcas_descriptor_layout, nullptr);
  
  if (m_descriptor_pool != VK_NULL_HANDLE)
    vkDestroyDescriptorPool(g_vulkan_context->GetDevice(), m_descriptor_pool, nullptr);
}

bool FSR1::IsSupported()
{
  // FSR1 requires compute shader support, which is available in Vulkan 1.0+
  // All modern Vulkan implementations support compute shaders
  return g_vulkan_context != nullptr;
}

bool FSR1::Initialize()
{
  if (m_initialized)
    return true;

  if (!CompileShaders())
  {
    ERROR_LOG_FMT(VIDEO, "FSR1: Failed to compile shaders");
    return false;
  }

  if (!CreateDescriptorSetLayouts())
  {
    ERROR_LOG_FMT(VIDEO, "FSR1: Failed to create descriptor set layouts");
    return false;
  }

  if (!CreatePipelines())
  {
    ERROR_LOG_FMT(VIDEO, "FSR1: Failed to create pipelines");
    return false;
  }

  m_initialized = true;
  INFO_LOG_FMT(VIDEO, "FSR1: Initialization successful");
  return true;
}

bool FSR1::CompileShaders()
{
  // Load and compile EASU shader
  std::string easu_shader_path = File::GetSysDirectory() + "Shaders/FSR1_EASU.glsl";
  std::string easu_source;
  if (!File::ReadFileToString(easu_shader_path, easu_source))
  {
    ERROR_LOG_FMT(VIDEO, "FSR1: Failed to read EASU shader from {}", easu_shader_path);
    return false;
  }

  auto easu_spirv = ShaderCompiler::CompileComputeShader(easu_source);
  if (!easu_spirv)
  {
    ERROR_LOG_FMT(VIDEO, "FSR1: Failed to compile EASU shader");
    return false;
  }

  m_easu_shader = VKShader::CreateFromBinary(ShaderStage::Compute, easu_spirv->data(),
                                              easu_spirv->size() * sizeof(u32), "FSR1_EASU");
  if (!m_easu_shader)
  {
    ERROR_LOG_FMT(VIDEO, "FSR1: Failed to create EASU shader module");
    return false;
  }

  // Load and compile RCAS shader
  std::string rcas_shader_path = File::GetSysDirectory() + "Shaders/FSR1_RCAS.glsl";
  std::string rcas_source;
  if (!File::ReadFileToString(rcas_shader_path, rcas_source))
  {
    ERROR_LOG_FMT(VIDEO, "FSR1: Failed to read RCAS shader from {}", rcas_shader_path);
    return false;
  }

  auto rcas_spirv = ShaderCompiler::CompileComputeShader(rcas_source);
  if (!rcas_spirv)
  {
    ERROR_LOG_FMT(VIDEO, "FSR1: Failed to compile RCAS shader");
    return false;
  }

  m_rcas_shader = VKShader::CreateFromBinary(ShaderStage::Compute, rcas_spirv->data(),
                                              rcas_spirv->size() * sizeof(u32), "FSR1_RCAS");
  if (!m_rcas_shader)
  {
    ERROR_LOG_FMT(VIDEO, "FSR1: Failed to create RCAS shader module");
    return false;
  }

  INFO_LOG_FMT(VIDEO, "FSR1: Shaders compiled successfully");
  return true;
}

bool FSR1::CreateDescriptorSetLayouts()
{
  VkDevice device = g_vulkan_context->GetDevice();

  // EASU descriptor set layout: input sampler, output image, uniform buffer
  std::array<VkDescriptorSetLayoutBinding, 3> easu_bindings = {};
  
  // Binding 0: Input texture sampler
  easu_bindings[0].binding = 0;
  easu_bindings[0].descriptorType = VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER;
  easu_bindings[0].descriptorCount = 1;
  easu_bindings[0].stageFlags = VK_SHADER_STAGE_COMPUTE_BIT;
  
  // Binding 1: Output image
  easu_bindings[1].binding = 1;
  easu_bindings[1].descriptorType = VK_DESCRIPTOR_TYPE_STORAGE_IMAGE;
  easu_bindings[1].descriptorCount = 1;
  easu_bindings[1].stageFlags = VK_SHADER_STAGE_COMPUTE_BIT;
  
  // Binding 2: Uniform buffer (constants)
  easu_bindings[2].binding = 2;
  easu_bindings[2].descriptorType = VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER;
  easu_bindings[2].descriptorCount = 1;
  easu_bindings[2].stageFlags = VK_SHADER_STAGE_COMPUTE_BIT;

  VkDescriptorSetLayoutCreateInfo easu_layout_info = {};
  easu_layout_info.sType = VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO;
  easu_layout_info.bindingCount = static_cast<u32>(easu_bindings.size());
  easu_layout_info.pBindings = easu_bindings.data();

  VkResult res = vkCreateDescriptorSetLayout(device, &easu_layout_info, nullptr,
                                              &m_easu_descriptor_layout);
  if (res != VK_SUCCESS)
  {
    ERROR_LOG_FMT(VIDEO, "FSR1: Failed to create EASU descriptor set layout: {}",
                  VkResultToString(res));
    return false;
  }

  // RCAS descriptor set layout: input sampler, output image, uniform buffer
  std::array<VkDescriptorSetLayoutBinding, 3> rcas_bindings = {};
  
  rcas_bindings[0].binding = 0;
  rcas_bindings[0].descriptorType = VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER;
  rcas_bindings[0].descriptorCount = 1;
  rcas_bindings[0].stageFlags = VK_SHADER_STAGE_COMPUTE_BIT;
  
  rcas_bindings[1].binding = 1;
  rcas_bindings[1].descriptorType = VK_DESCRIPTOR_TYPE_STORAGE_IMAGE;
  rcas_bindings[1].descriptorCount = 1;
  rcas_bindings[1].stageFlags = VK_SHADER_STAGE_COMPUTE_BIT;
  
  rcas_bindings[2].binding = 2;
  rcas_bindings[2].descriptorType = VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER;
  rcas_bindings[2].descriptorCount = 1;
  rcas_bindings[2].stageFlags = VK_SHADER_STAGE_COMPUTE_BIT;

  VkDescriptorSetLayoutCreateInfo rcas_layout_info = {};
  rcas_layout_info.sType = VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO;
  rcas_layout_info.bindingCount = static_cast<u32>(rcas_bindings.size());
  rcas_layout_info.pBindings = rcas_bindings.data();

  res = vkCreateDescriptorSetLayout(device, &rcas_layout_info, nullptr,
                                     &m_rcas_descriptor_layout);
  if (res != VK_SUCCESS)
  {
    ERROR_LOG_FMT(VIDEO, "FSR1: Failed to create RCAS descriptor set layout: {}",
                  VkResultToString(res));
    return false;
  }

  return true;
}

bool FSR1::CreatePipelines()
{
  VkDevice device = g_vulkan_context->GetDevice();

  // Create EASU pipeline layout
  VkPipelineLayoutCreateInfo easu_layout_info = {};
  easu_layout_info.sType = VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO;
  easu_layout_info.setLayoutCount = 1;
  easu_layout_info.pSetLayouts = &m_easu_descriptor_layout;

  VkResult res = vkCreatePipelineLayout(device, &easu_layout_info, nullptr,
                                         &m_easu_pipeline_layout);
  if (res != VK_SUCCESS)
  {
    ERROR_LOG_FMT(VIDEO, "FSR1: Failed to create EASU pipeline layout: {}",
                  VkResultToString(res));
    return false;
  }

  // Create EASU compute pipeline
  VkComputePipelineCreateInfo easu_pipeline_info = {};
  easu_pipeline_info.sType = VK_STRUCTURE_TYPE_COMPUTE_PIPELINE_CREATE_INFO;
  easu_pipeline_info.stage.sType = VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO;
  easu_pipeline_info.stage.stage = VK_SHADER_STAGE_COMPUTE_BIT;
  easu_pipeline_info.stage.module = static_cast<const VKShader*>(m_easu_shader.get())->GetShaderModule();
  easu_pipeline_info.stage.pName = "main";
  easu_pipeline_info.layout = m_easu_pipeline_layout;

  res = vkCreateComputePipelines(device, VK_NULL_HANDLE, 1, &easu_pipeline_info, nullptr,
                                  &m_easu_pipeline);
  if (res != VK_SUCCESS)
  {
    ERROR_LOG_FMT(VIDEO, "FSR1: Failed to create EASU compute pipeline: {}",
                  VkResultToString(res));
    return false;
  }

  // Create RCAS pipeline layout
  VkPipelineLayoutCreateInfo rcas_layout_info = {};
  rcas_layout_info.sType = VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO;
  rcas_layout_info.setLayoutCount = 1;
  rcas_layout_info.pSetLayouts = &m_rcas_descriptor_layout;

  res = vkCreatePipelineLayout(device, &rcas_layout_info, nullptr,
                                &m_rcas_pipeline_layout);
  if (res != VK_SUCCESS)
  {
    ERROR_LOG_FMT(VIDEO, "FSR1: Failed to create RCAS pipeline layout: {}",
                  VkResultToString(res));
    return false;
  }

  // Create RCAS compute pipeline
  VkComputePipelineCreateInfo rcas_pipeline_info = {};
  rcas_pipeline_info.sType = VK_STRUCTURE_TYPE_COMPUTE_PIPELINE_CREATE_INFO;
  rcas_pipeline_info.stage.sType = VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO;
  rcas_pipeline_info.stage.stage = VK_SHADER_STAGE_COMPUTE_BIT;
  rcas_pipeline_info.stage.module = static_cast<const VKShader*>(m_rcas_shader.get())->GetShaderModule();
  rcas_pipeline_info.stage.pName = "main";
  rcas_pipeline_info.layout = m_rcas_pipeline_layout;

  res = vkCreateComputePipelines(device, VK_NULL_HANDLE, 1, &rcas_pipeline_info, nullptr,
                                  &m_rcas_pipeline);
  if (res != VK_SUCCESS)
  {
    ERROR_LOG_FMT(VIDEO, "FSR1: Failed to create RCAS compute pipeline: {}",
                  VkResultToString(res));
    return false;
  }

  INFO_LOG_FMT(VIDEO, "FSR1: Pipelines created successfully");
  return true;
}

void FSR1::ApplyFSR1(VKTexture* source, VKTexture* dest, float sharpness)
{
  if (!m_initialized)
  {
    WARN_LOG_FMT(VIDEO, "FSR1: Attempted to apply FSR1 before initialization");
    return;
  }

  // For now, implement a simple copy as placeholder
  // Full implementation requires intermediate texture and descriptor sets
  // This will be implemented in the next step
  
  WARN_LOG_FMT(VIDEO, "FSR1: ApplyFSR1 not yet fully implemented");
}

void FSR1::ApplyEASU(VKTexture* source, VKTexture* intermediate)
{
  // TODO: Implement EASU pass
}

void FSR1::ApplyRCAS(VKTexture* intermediate, VKTexture* dest, float sharpness)
{
  // TODO: Implement RCAS pass
}

}  // namespace Vulkan
