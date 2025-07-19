// SPDX-License-Identifier: GPL-2.0-or-later

package org.dolphinemu.dolphinemu.features.settings.model.view;

import android.content.Context;

import org.dolphinemu.dolphinemu.features.settings.model.AbstractSetting;

public class HeaderSetting extends SettingsItem
{
  public enum Style
  {
    NORMAL,
    CENTERED
  }

  private final Style style;

  public HeaderSetting(Context context, int titleId, int descriptionId)
  {
    super(context, titleId, descriptionId);
    this.style = Style.NORMAL;
  }

  private HeaderSetting(Context context, int titleId, int descriptionId, Style style)
  {
    super(context, titleId, descriptionId);
    this.style = style;
  }

   public static HeaderSetting centered(Context context, int titleId, int descriptionId)
  {
    return new HeaderSetting(context, titleId, descriptionId, Style.CENTERED);
  }

  @Override
  public int getType()
  {
    return (style == Style.CENTERED) ? TYPE_HEADER_CENTERED : TYPE_HEADER;
  }

  @Override
  public AbstractSetting getSetting()
  {
    return null;
  }
}
