// SPDX-License-Identifier: GPL-2.0-or-later

package org.dolphinemu.dolphinemu.features.settings.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.provider.DocumentsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import org.dolphinemu.dolphinemu.R;
import org.dolphinemu.dolphinemu.databinding.ListItemSettingBinding;
import org.dolphinemu.dolphinemu.databinding.ListItemSettingCheckboxBinding;
import org.dolphinemu.dolphinemu.databinding.ListItemSubmenuBinding;
import org.dolphinemu.dolphinemu.dialogs.MotionAlertDialog;
import org.dolphinemu.dolphinemu.features.settings.model.IntSetting;
import org.dolphinemu.dolphinemu.features.settings.model.Settings;
import org.dolphinemu.dolphinemu.features.settings.model.view.CheckBoxSetting;
import org.dolphinemu.dolphinemu.features.settings.model.view.FilePicker;
import org.dolphinemu.dolphinemu.features.settings.model.view.FloatSliderSetting;
import org.dolphinemu.dolphinemu.features.settings.model.view.InputBindingSetting;
import org.dolphinemu.dolphinemu.features.settings.model.view.InputStringSetting;
import org.dolphinemu.dolphinemu.features.settings.model.view.IntSliderSetting;
import org.dolphinemu.dolphinemu.features.settings.model.view.RumbleBindingSetting;
import org.dolphinemu.dolphinemu.features.settings.model.view.SettingsItem;
import org.dolphinemu.dolphinemu.features.settings.model.view.SingleChoiceSetting;
import org.dolphinemu.dolphinemu.features.settings.model.view.SingleChoiceSettingDynamicDescriptions;
import org.dolphinemu.dolphinemu.features.settings.model.view.SliderSetting;
import org.dolphinemu.dolphinemu.features.settings.model.view.StringSingleChoiceSetting;
import org.dolphinemu.dolphinemu.features.settings.model.view.SubmenuSetting;
import org.dolphinemu.dolphinemu.features.settings.ui.viewholder.CheckBoxSettingViewHolder;
import org.dolphinemu.dolphinemu.features.settings.ui.viewholder.FilePickerViewHolder;
import org.dolphinemu.dolphinemu.features.settings.ui.viewholder.HeaderHyperLinkViewHolder;
import org.dolphinemu.dolphinemu.features.settings.ui.viewholder.HeaderViewHolder;
import org.dolphinemu.dolphinemu.features.settings.ui.viewholder.InputBindingSettingViewHolder;
import org.dolphinemu.dolphinemu.features.settings.ui.viewholder.InputStringSettingViewHolder;
import org.dolphinemu.dolphinemu.features.settings.ui.viewholder.RumbleBindingViewHolder;
import org.dolphinemu.dolphinemu.features.settings.ui.viewholder.RunRunnableViewHolder;
import org.dolphinemu.dolphinemu.features.settings.ui.viewholder.SettingViewHolder;
import org.dolphinemu.dolphinemu.features.settings.ui.viewholder.SingleChoiceViewHolder;
import org.dolphinemu.dolphinemu.features.settings.ui.viewholder.SliderSelectorViewHolder;
import org.dolphinemu.dolphinemu.features.settings.ui.viewholder.SliderViewHolder;
import org.dolphinemu.dolphinemu.features.settings.ui.viewholder.SubmenuViewHolder;
import org.dolphinemu.dolphinemu.utils.DirectoryInitialization;
import org.dolphinemu.dolphinemu.utils.FileBrowserHelper;
import org.dolphinemu.dolphinemu.utils.Log;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

public final class SettingsAdapter extends RecyclerView.Adapter<SettingViewHolder>
        implements DialogInterface.OnClickListener, SeekBar.OnSeekBarChangeListener
{
  private static final int GESTURE_ACTION_DPAD = 1000;
  private static final int GESTURE_ACTION_STICK = 1001;
  private final SettingsFragmentView mView;
  private final Context mContext;
  private ArrayList<SettingsItem> mSettings;

  private SettingsItem mClickedItem;
  private int mClickedPosition;
  private int mSeekbarMinValue;
  private int mSeekbarProgress;

  private AlertDialog mDialog;
  private TextView mTextSliderValue;

  public SettingsAdapter(SettingsFragmentView view, Context context)
  {
    mView = view;
    mContext = context;
    mClickedPosition = -1;
  }

  public SettingsFragmentView getView()
  {
    return mView;
  }

  @Override
  public SettingViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
  {
    View view;
    LayoutInflater inflater = LayoutInflater.from(parent.getContext());

    switch (viewType)
    {
      case SettingsItem.TYPE_HEADER:
        view = inflater.inflate(R.layout.list_item_header, parent, false);
        return new HeaderViewHolder(view, this);

      case SettingsItem.TYPE_HEADER_CENTERED:
        view = inflater.inflate(R.layout.list_item_header_centered, parent, false);
        return new HeaderViewHolder(view, this);

      case SettingsItem.TYPE_CHECKBOX:
        return new CheckBoxSettingViewHolder(ListItemSettingCheckboxBinding.inflate(inflater),
          this);

      case SettingsItem.TYPE_STRING_SINGLE_CHOICE:
      case SettingsItem.TYPE_SINGLE_CHOICE_DYNAMIC_DESCRIPTIONS:
      case SettingsItem.TYPE_SINGLE_CHOICE:
        return new SingleChoiceViewHolder(ListItemSettingBinding.inflate(inflater), this);

      case SettingsItem.TYPE_SLIDER:
        return new SliderViewHolder(ListItemSettingBinding.inflate(inflater), this, mContext);

      case SettingsItem.TYPE_SLIDER_SELECTOR:
        view = inflater.inflate(R.layout.list_item_setting_slider_selector, parent, false);
        return new SliderSelectorViewHolder(view, this);

      case SettingsItem.TYPE_SUBMENU:
        return new SubmenuViewHolder(ListItemSubmenuBinding.inflate(inflater), this);

      case SettingsItem.TYPE_INPUT_BINDING:
        return new InputBindingSettingViewHolder(ListItemSettingBinding.inflate(inflater), this,
                mContext);

      case SettingsItem.TYPE_RUMBLE_BINDING:
        return new RumbleBindingViewHolder(ListItemSettingBinding.inflate(inflater), this,
                mContext);

      case SettingsItem.TYPE_FILE_PICKER:
        return new FilePickerViewHolder(ListItemSettingBinding.inflate(inflater), this);

      case SettingsItem.TYPE_RUN_RUNNABLE:
        return new RunRunnableViewHolder(ListItemSettingBinding.inflate(inflater), this, mContext);

      case SettingsItem.TYPE_STRING:
        return new InputStringSettingViewHolder(ListItemSettingBinding.inflate(inflater), this);

      case SettingsItem.TYPE_HYPERLINK_HEADER:
        view = inflater.inflate(R.layout.list_item_header, parent, false);
        return new HeaderHyperLinkViewHolder(view, this, mContext);

      default:
        throw new IllegalArgumentException("Invalid view type: " + viewType);
    }
  }

  @Override
  public void onBindViewHolder(SettingViewHolder holder, int position)
  {
    holder.bind(getItem(position));
  }

  private SettingsItem getItem(int position)
  {
    return mSettings.get(position);
  }

  @Override
  public int getItemCount()
  {
    if (mSettings != null)
    {
      return mSettings.size();
    }
    else
    {
      return 0;
    }
  }

  @Override
  public int getItemViewType(int position)
  {
    return getItem(position).getType();
  }

  public Settings getSettings()
  {
    return mView.getSettings();
  }

  public void setSettings(ArrayList<SettingsItem> settings)
  {
    mSettings = settings;
    notifyDataSetChanged();
  }

  public void clearSetting(SettingsItem item, int position)
  {
    item.clear(getSettings());
    notifyItemChanged(position);

    mView.onSettingChanged();
  }

  public void notifyAllSettingsChanged()
  {
    notifyItemRangeChanged(0, getItemCount());

    mView.onSettingChanged();
  }

  public void onBooleanClick(CheckBoxSetting item, int position, boolean checked)
  {
    item.setChecked(getSettings(), checked);
    notifyItemChanged(position);

    mView.onSettingChanged();
  }

  public void onInputStringClick(InputStringSetting item, int position)
  {
    LayoutInflater inflater = LayoutInflater.from(mContext);

    AlertDialog.Builder builder = new AlertDialog.Builder(mContext, R.style.DolphinDialogBase);
    View dialogView = inflater.inflate(R.layout.dialog_input_string, null);
    EditText input = (EditText) dialogView.findViewById(R.id.input);
    input.setText(item.getSelectedValue(getSettings()));

    builder.setView(dialogView);
    builder.setMessage(item.getDescription());
    builder.setPositiveButton(R.string.ok, (dialogInterface, i) ->
    {
      String editTextInput = input.getText().toString();

      if (!item.getSelectedValue(mView.getSettings()).equals(editTextInput))
      {
        notifyItemChanged(position);
        mView.onSettingChanged();
      }

      item.setSelectedValue(mView.getSettings(), editTextInput);
    });
    builder.setNegativeButton(R.string.cancel, null);

    mDialog = builder.show();
  }

  public void onSingleChoiceClick(SingleChoiceSetting item, int position)
  {
    mClickedItem = item;
    mClickedPosition = position;

    int value = getSelectionForSingleChoiceValue(item);

    AlertDialog.Builder builder = new AlertDialog.Builder(mView.getActivity());

    builder.setTitle(item.getName());
    builder.setSingleChoiceItems(item.getChoicesId(), value, this);

    mDialog = builder.show();
  }

  public void onStringSingleChoiceClick(StringSingleChoiceSetting item, int position)
  {
    mClickedItem = item;
    mClickedPosition = position;

    AlertDialog.Builder builder = new AlertDialog.Builder(mView.getActivity());

    builder.setTitle(item.getName());
    builder.setSingleChoiceItems(item.getChoices(), item.getSelectedValueIndex(getSettings()),
            this);

    mDialog = builder.show();
  }

  public void onSingleChoiceDynamicDescriptionsClick(SingleChoiceSettingDynamicDescriptions item,
          int position)
  {
    mClickedItem = item;
    mClickedPosition = position;

    int value = getSelectionForSingleChoiceDynamicDescriptionsValue(item);

    AlertDialog.Builder builder = new AlertDialog.Builder(mView.getActivity());

    builder.setTitle(item.getName());
    builder.setSingleChoiceItems(item.getChoicesId(), value, this);

    mDialog = builder.show();
  }

  public void onSliderClick(SliderSetting item, int position)
  {
    mClickedItem = item;
    mClickedPosition = position;
    mSeekbarMinValue = item.getMin();
    mSeekbarProgress = item.getSelectedValue(getSettings());
    AlertDialog.Builder builder = new AlertDialog.Builder(mView.getActivity());

    LayoutInflater inflater = LayoutInflater.from(mView.getActivity());
    View view = inflater.inflate(R.layout.dialog_seekbar, null);

    builder.setTitle(item.getName());
    builder.setView(view);
    builder.setPositiveButton(R.string.ok, this);
    mDialog = builder.show();

    mTextSliderValue = view.findViewById(R.id.text_value);
    mTextSliderValue.setText(String.valueOf(mSeekbarProgress));

    TextView units = view.findViewById(R.id.text_units);
    units.setText(item.getUnits());

    SeekBar seekbar = view.findViewById(R.id.seekbar);

    // TODO: Once we require API 26, uncomment this line and remove the mSeekbarMinValue variable
    //seekbar.setMin(item.getMin());

    seekbar.setMax(item.getMax() - mSeekbarMinValue);
    seekbar.setProgress(mSeekbarProgress - mSeekbarMinValue);
    seekbar.setKeyProgressIncrement(5);

    seekbar.setOnSeekBarChangeListener(this);
  }

  public void onSubmenuClick(SubmenuSetting item)
  {
    mView.loadSubMenu(item.getMenuKey());
  }

  public void onInputBindingClick(final InputBindingSetting item, final int position)
  {
    final MotionAlertDialog dialog = new MotionAlertDialog(mContext, item, this);
    dialog.setTitle(R.string.input_binding);
    dialog.setMessage(String.format(mContext.getString(
                    item instanceof RumbleBindingSetting ?
                            R.string.input_rumble_description : R.string.input_binding_description),
            item.getName()));
    dialog.setButton(AlertDialog.BUTTON_NEGATIVE, mContext.getString(R.string.cancel), this);
    dialog.setButton(AlertDialog.BUTTON_NEUTRAL, mContext.getString(R.string.clear),
            (dialogInterface, i) -> item.clearValue(getSettings()));
    dialog.setOnDismissListener(dialog1 ->
    {
      notifyItemChanged(position);
      mView.onSettingChanged();
    });
    dialog.setCanceledOnTouchOutside(false);
    dialog.show();
  }

  public void onFilePickerDirectoryClick(SettingsItem item, int position)
  {
    mClickedItem = item;
    mClickedPosition = position;

    FileBrowserHelper.openDirectoryPicker(mView.getActivity(), FileBrowserHelper.GAME_EXTENSIONS);
  }

  public void onFilePickerFileClick(SettingsItem item, int position)
  {
    mClickedItem = item;
    mClickedPosition = position;
    FilePicker filePicker = (FilePicker) item;

    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
    intent.addCategory(Intent.CATEGORY_OPENABLE);
    intent.setType("*/*");

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
    {
      intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI,
              filePicker.getSelectedValue(mView.getSettings()));
    }

    mView.getActivity().startActivityForResult(intent, filePicker.getRequestType());
  }

  public void onFilePickerConfirmation(String selectedFile)
  {
    FilePicker filePicker = (FilePicker) mClickedItem;

    if (!filePicker.getSelectedValue(mView.getSettings()).equals(selectedFile))
    {
      notifyItemChanged(mClickedPosition);
      mView.onSettingChanged();
    }

    filePicker.setSelectedValue(mView.getSettings(), selectedFile);

    mClickedItem = null;
  }

  public static void clearLog()
  {
    // Don't delete the log in case it is being monitored by another app.
    File log = new File(DirectoryInitialization.getUserDirectory() + "/Logs/dolphin.log");

    try
    {
      RandomAccessFile raf = new RandomAccessFile(log, "rw");
      raf.setLength(0);
    }
    catch (IOException e)
    {
      Log.error("[SettingsAdapter] Failed to clear log file: " + e.getMessage());
    }
  }

  private void handleMenuTag(MenuTag menuTag, int value)
  {
    if (menuTag != null)
    {
      if (menuTag.isSerialPort1Menu())
      {
        mView.onSerialPort1SettingChanged(menuTag, value);
      }

      if (menuTag.isGCPadMenu())
      {
        mView.onGcPadSettingChanged(menuTag, value);
      }

      if (menuTag.isWiimoteMenu())
      {
        mView.onWiimoteSettingChanged(menuTag, value);
      }

      if (menuTag.isWiimoteExtensionMenu())
      {
        mView.onExtensionSettingChanged(menuTag, value);
      }
    }
  }

  @Override
  public void onClick(DialogInterface dialog, int which)
  {
    if (mClickedItem instanceof SingleChoiceSetting)
    {
      SingleChoiceSetting scSetting = (SingleChoiceSetting) mClickedItem;

      int value = getValueForSingleChoiceSelection(scSetting, which);
      if (scSetting.getSelectedValue(getSettings()) != value)
        mView.onSettingChanged();

      handleMenuTag(scSetting.getMenuTag(), value);

      scSetting.setSelectedValue(getSettings(), value);
      applyGestureAutoFill(scSetting, value);

      closeDialog();
    }
    else if (mClickedItem instanceof SingleChoiceSettingDynamicDescriptions)
    {
      SingleChoiceSettingDynamicDescriptions scSetting =
              (SingleChoiceSettingDynamicDescriptions) mClickedItem;

      int value = getValueForSingleChoiceDynamicDescriptionsSelection(scSetting, which);
      if (scSetting.getSelectedValue(getSettings()) != value)
        mView.onSettingChanged();

      scSetting.setSelectedValue(getSettings(), value);

      closeDialog();
    }
    else if (mClickedItem instanceof StringSingleChoiceSetting)
    {
      StringSingleChoiceSetting scSetting = (StringSingleChoiceSetting) mClickedItem;
      String value = scSetting.getValueAt(which);
      if (!scSetting.getSelectedValue(getSettings()).equals(value))
        mView.onSettingChanged();

      handleMenuTag(scSetting.getMenuTag(), which);

      scSetting.setSelectedValue(getSettings(), value);

      closeDialog();
    }
    else if (mClickedItem instanceof IntSliderSetting)
    {
      IntSliderSetting sliderSetting = (IntSliderSetting) mClickedItem;
      if (sliderSetting.getSelectedValue(getSettings()) != mSeekbarProgress)
        mView.onSettingChanged();

      sliderSetting.setSelectedValue(getSettings(), mSeekbarProgress);

      closeDialog();
    }
    else if (mClickedItem instanceof FloatSliderSetting)
    {
      FloatSliderSetting sliderSetting = (FloatSliderSetting) mClickedItem;
      if (sliderSetting.getSelectedValue(getSettings()) != mSeekbarProgress)
        mView.onSettingChanged();

      sliderSetting.setSelectedValue(getSettings(), mSeekbarProgress);

      closeDialog();
    }

    mClickedItem = null;
    mSeekbarProgress = -1;
  }

  private void applyGestureAutoFill(SingleChoiceSetting setting, int value)
  {
    if (value != GESTURE_ACTION_DPAD && value != GESTURE_ACTION_STICK)
      return;

    if (!(setting.getSetting() instanceof IntSetting))
      return;

    IntSetting intSetting = (IntSetting) setting.getSetting();
    Settings settings = getSettings();

    if (intSetting == IntSetting.GESTURE_LEFT_SWIPE_UP ||
            intSetting == IntSetting.GESTURE_LEFT_SWIPE_DOWN ||
            intSetting == IntSetting.GESTURE_LEFT_SWIPE_LEFT ||
            intSetting == IntSetting.GESTURE_LEFT_SWIPE_RIGHT)
    {
      IntSetting.GESTURE_LEFT_SWIPE_UP.setInt(settings, value);
      IntSetting.GESTURE_LEFT_SWIPE_DOWN.setInt(settings, value);
      IntSetting.GESTURE_LEFT_SWIPE_LEFT.setInt(settings, value);
      IntSetting.GESTURE_LEFT_SWIPE_RIGHT.setInt(settings, value);
      mView.onSettingChanged();
      notifyDataSetChanged();
      return;
    }

    if (intSetting == IntSetting.GESTURE_RIGHT_SWIPE_UP ||
            intSetting == IntSetting.GESTURE_RIGHT_SWIPE_DOWN ||
            intSetting == IntSetting.GESTURE_RIGHT_SWIPE_LEFT ||
            intSetting == IntSetting.GESTURE_RIGHT_SWIPE_RIGHT)
    {
      IntSetting.GESTURE_RIGHT_SWIPE_UP.setInt(settings, value);
      IntSetting.GESTURE_RIGHT_SWIPE_DOWN.setInt(settings, value);
      IntSetting.GESTURE_RIGHT_SWIPE_LEFT.setInt(settings, value);
      IntSetting.GESTURE_RIGHT_SWIPE_RIGHT.setInt(settings, value);
      mView.onSettingChanged();
      notifyDataSetChanged();
    }
  }

  public void closeDialog()
  {
    if (mDialog != null)
    {
      if (mClickedPosition != -1)
      {
        notifyItemChanged(mClickedPosition);
        mClickedPosition = -1;
      }
      mDialog.dismiss();
      mDialog = null;
    }
  }

  @Override
  public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
  {
    mSeekbarProgress = seekBar.getMax() > 99 ? (progress / 5) * 5 : progress;
    mTextSliderValue.setText(String.valueOf(mSeekbarProgress));
  }

  @Override
  public void onStartTrackingTouch(SeekBar seekBar)
  {
  }

  @Override
  public void onStopTrackingTouch(SeekBar seekBar)
  {
  }

  private int getValueForSingleChoiceSelection(SingleChoiceSetting item, int which)
  {
    int valuesId = item.getValuesId();

    if (valuesId > 0)
    {
      int[] valuesArray = mContext.getResources().getIntArray(valuesId);
      return valuesArray[which];
    }
    else
    {
      return which;
    }
  }

  private int getSelectionForSingleChoiceValue(SingleChoiceSetting item)
  {
    int value = item.getSelectedValue(getSettings());
    int valuesId = item.getValuesId();

    if (valuesId > 0)
    {
      int[] valuesArray = mContext.getResources().getIntArray(valuesId);
      for (int index = 0; index < valuesArray.length; index++)
      {
        int current = valuesArray[index];
        if (current == value)
        {
          return index;
        }
      }
    }
    else
    {
      return value;
    }

    return -1;
  }

  private int getValueForSingleChoiceDynamicDescriptionsSelection(
          SingleChoiceSettingDynamicDescriptions item, int which)
  {
    int valuesId = item.getValuesId();

    if (valuesId > 0)
    {
      int[] valuesArray = mContext.getResources().getIntArray(valuesId);
      return valuesArray[which];
    }
    else
    {
      return which;
    }
  }

  private int getSelectionForSingleChoiceDynamicDescriptionsValue(
          SingleChoiceSettingDynamicDescriptions item)
  {
    int value = item.getSelectedValue(getSettings());
    int valuesId = item.getValuesId();

    if (valuesId > 0)
    {
      int[] valuesArray = mContext.getResources().getIntArray(valuesId);
      for (int index = 0; index < valuesArray.length; index++)
      {
        int current = valuesArray[index];
        if (current == value)
        {
          return index;
        }
      }
    }
    else
    {
      return value;
    }

    return -1;
  }
}
