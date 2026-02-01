// Copyright 2026 Dolphin Emulator Project
// SPDX-License-Identifier: GPL-2.0-or-later

#include "jni/AndroidCommon/CrashLogger.h"

#include <android/log.h>
#include <fcntl.h>
#include <limits.h>
#include <signal.h>
#include <unistd.h>

#include <cstring>
#include <string>

namespace AndroidCommon
{
namespace
{
constexpr int kHandledSignals[] = {SIGABRT, SIGBUS, SIGFPE, SIGILL, SIGSEGV};

int s_crash_fd = -1;
volatile sig_atomic_t s_handling_crash = 0;

void WriteString(const char* text)
{
  if (s_crash_fd < 0 || text == nullptr)
    return;

  size_t len = 0;
  while (text[len] != '\0')
    ++len;

  if (len > 0)
    write(s_crash_fd, text, len);
}

void WriteDecimal(long value)
{
  if (s_crash_fd < 0)
    return;

  char buffer[32];
  size_t pos = 0;

  if (value == 0)
  {
    buffer[pos++] = '0';
  }
  else
  {
    long v = value;
    if (v < 0)
    {
      buffer[pos++] = '-';
      v = -v;
    }

    char digits[20];
    size_t digits_len = 0;
    while (v > 0 && digits_len < sizeof(digits))
    {
      digits[digits_len++] = static_cast<char>('0' + (v % 10));
      v /= 10;
    }

    for (size_t i = 0; i < digits_len; ++i)
      buffer[pos++] = digits[digits_len - i - 1];
  }

  write(s_crash_fd, buffer, pos);
}

void WriteHex(uintptr_t value)
{
  if (s_crash_fd < 0)
    return;

  char buffer[2 + sizeof(uintptr_t) * 2];
  size_t pos = 0;
  buffer[pos++] = '0';
  buffer[pos++] = 'x';

  for (size_t i = 0; i < sizeof(uintptr_t) * 2; ++i)
  {
    const size_t shift = (sizeof(uintptr_t) * 2 - 1 - i) * 4;
    const uint8_t nibble = static_cast<uint8_t>((value >> shift) & 0xF);
    buffer[pos++] = static_cast<char>(nibble < 10 ? ('0' + nibble) : ('a' + nibble - 10));
  }

  write(s_crash_fd, buffer, pos);
}

void CrashHandler(int signum, siginfo_t* info, void* context)
{
  (void)context;

  if (s_handling_crash)
    _exit(128 + signum);

  s_handling_crash = 1;

  WriteString("\n===== Dolphin Android Crash =====\n");
  WriteString("signal: ");
  WriteDecimal(signum);
  WriteString("\n");

  if (info)
  {
    WriteString("code: ");
    WriteDecimal(info->si_code);
    WriteString("\naddr: ");
    WriteHex(reinterpret_cast<uintptr_t>(info->si_addr));
    WriteString("\n");
  }

  WriteString("(No stack trace captured in signal handler)\n");

  if (s_crash_fd >= 0)
    close(s_crash_fd);

  signal(signum, SIG_DFL);
  raise(signum);
}
}  // namespace

void InstallCrashHandler(const std::string& log_path)
{
  if (log_path.empty())
    return;

  if (s_crash_fd >= 0)
  {
    close(s_crash_fd);
    s_crash_fd = -1;
  }

  s_crash_fd = open(log_path.c_str(), O_CREAT | O_WRONLY | O_TRUNC | O_CLOEXEC, 0644);
  if (s_crash_fd < 0)
  {
    __android_log_print(ANDROID_LOG_ERROR, "Dolphin", "Failed to open crash log: %s",
                        log_path.c_str());
    return;
  }

  WriteString("Dolphin Android crash log initialized.\n");

  struct sigaction action;
  std::memset(&action, 0, sizeof(action));
  action.sa_sigaction = CrashHandler;
  action.sa_flags = SA_SIGINFO | SA_RESETHAND;

  sigemptyset(&action.sa_mask);
  for (int sig : kHandledSignals)
    sigaction(sig, &action, nullptr);
}
}  // namespace AndroidCommon
