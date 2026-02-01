// SPDX-License-Identifier: GPL-2.0-or-later

package org.dolphinemu.dolphinemu.utils;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

public final class CrashLogger
{
  private CrashLogger()
  {
  }

  public static void install(Context context, String userPath)
  {
    final Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
    Thread.setDefaultUncaughtExceptionHandler((thread, throwable) ->
    {
      writeCrashLog(context, userPath, thread, throwable);
      if (previous != null)
      {
        previous.uncaughtException(thread, throwable);
      }
    });
  }

  private static void writeCrashLog(Context context, String userPath, Thread thread, Throwable t)
  {
    StringWriter stringWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter(stringWriter);
    printWriter.println("===== Dolphin Java Crash =====");
    printWriter.println("thread: " + thread.getName());
    t.printStackTrace(printWriter);
    printWriter.flush();

    String payload = stringWriter.toString();

    if (context != null)
    {
      File internalLog = new File(context.getFilesDir(), "crash-java.log");
      appendToFile(internalLog, payload);
    }

    if (userPath != null)
    {
      File logsDir = new File(userPath, "Logs");
      //noinspection ResultOfMethodCallIgnored
      logsDir.mkdirs();
      File externalLog = new File(logsDir, "crash-java.log");
      appendToFile(externalLog, payload);
    }
  }

  private static void appendToFile(File file, String payload)
  {
    try (FileOutputStream stream = new FileOutputStream(file, true))
    {
      stream.write(payload.getBytes());
      stream.write('\n');
    }
    catch (Exception ignored)
    {
    }
  }
}
