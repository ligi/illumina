/*
 * illumina, a pilight remote
 *
 * Copyright (c) 2014 Peter Heisig <http://google.com/+PeterHeisig>
 *                    CurlyMo <http://www.pilight.org>
 *
 * illumina is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * illumina is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with illumina. If not, see <http://www.gnu.org/licenses/>.
 */

package nl.pilight.illumina.widget;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Filter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import nl.pilight.illumina.R;
import nl.pilight.illumina.pilight.Device;

public class DeviceAdapter extends ArrayAdapter<Device> {

    private DeviceChangeListener mDeviceChangeListener;

    private List<Device> mOriginalDeviceList;

    private Filter mFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence charSequence) {
            final FilterResults result = new FilterResults();
            final List<Device> filteredDeviceList = new ArrayList<>();

            for (Device device : mOriginalDeviceList) {
                if (!device.isWritable() && device.getType() == Device.DeviceTypes.SCREEN) {
                    continue;
                }

                filteredDeviceList.add(device);
            }

            result.values = filteredDeviceList;
            result.count = filteredDeviceList.size();

            return result;
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
            clear();

            addAll((ArrayList<Device>) filterResults.values);

            if (filterResults.count > 0) {
                notifyDataSetChanged();
            } else {
                notifyDataSetInvalidated();
            }
        }
    };

    public interface DeviceChangeListener {
        void onDeviceChange(Device device, int property);
    }

    public DeviceAdapter(Context context, List<Device> objects,
                DeviceChangeListener deviceChangeListener) {
        super(context, 0, objects);

        mDeviceChangeListener = deviceChangeListener;
        mOriginalDeviceList = objects;

        final TypedArray typedArray = context.obtainStyledAttributes(
                new int[]{R.attr.battery_full, R.attr.battery_empty});

        assert typedArray != null;
        WeatherViewHolder.setBatteryDrawables(
                context.getResources().getDrawable(typedArray.getResourceId(0, 0)),
                context.getResources().getDrawable(typedArray.getResourceId(1, 0))
        );

        typedArray.recycle();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final LayoutInflater inflater = LayoutInflater.from(getContext());
        final Device device = getItem(position);
        final Device.DeviceTypes type = device.getType();

        View view = convertView;
        DeviceViewHolder viewHolder = null;

        if (view == null) {
            switch (type) {
                case SWITCH:
                    view = inflater.inflate(R.layout.device_list_item_switch, parent, false);
                    viewHolder = new SwitchViewHolder(view);
                    break;

                case CONTACT:
                    view = inflater.inflate(R.layout.device_list_item_contact, parent, false);
                    viewHolder = new ContactViewHolder(view);
                    break;

                case DIMMER:
                    view = inflater.inflate(R.layout.device_list_item_dimmer, parent, false);
                    viewHolder = new DimmerViewHolder(view);
                    break;

                case SCREEN:
                    view = inflater.inflate(R.layout.device_list_item_screen, parent, false);
                    viewHolder = new ScreenViewHolder(view);
                    break;

                case WEATHER:
                    view = inflater.inflate(R.layout.device_list_item_weather, parent, false);
                    viewHolder = new WeatherViewHolder(view);
                    break;

                case UNKNOWN:
                    view = inflater.inflate(R.layout.device_list_item_unknown, parent, false);
                    viewHolder = new UnknownViewHolder(view);
                    break;
            }

            assert view != null;
            view.setTag(viewHolder);

        } else {
            viewHolder = (DeviceViewHolder) view.getTag();
        }

        assert viewHolder != null;

        viewHolder.setDevice(device);
        viewHolder.setDeviceChangeListener(mDeviceChangeListener);

        return view;
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position).getType().ordinal();
    }

    @Override
    public int getViewTypeCount() {
        return 5;
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).getId().hashCode();
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }

    @Override
    public boolean isEnabled(int position) {
        final Device device = getItem(position);
        return device != null && device.isWritable();
    }

    @Override
    public Filter getFilter() {
        return mFilter;
    }

    private static abstract class DeviceViewHolder {

        private Device mDevice;

        private TextView mName;

        private DeviceChangeListener mDeviceChangeListener;

        DeviceViewHolder(View view) {
            mName = (TextView) view.findViewById(android.R.id.text1);
        }

        void setDevice(Device device) {
            mDevice = device;
            mName.setText(device.getName());
            mName.setTypeface(mName.getTypeface(), device.isWritable()
                    ? Typeface.NORMAL : Typeface.ITALIC);
        }

        Device getDevice() {
            return mDevice;
        }

        void setDeviceChangeListener(DeviceChangeListener deviceChangeListener) {
            mDeviceChangeListener = deviceChangeListener;
        }

        DeviceChangeListener getDeviceChangeListener() {
            return mDeviceChangeListener;
        }

    }

    private static class SwitchViewHolder extends DeviceViewHolder {

        private CheckBox mCheckBox;

        SwitchViewHolder(View view) {
            super(view);
            mCheckBox = (CheckBox) view.findViewById(android.R.id.checkbox);
        }

        void setDevice(Device device) {
            super.setDevice(device);

            mCheckBox.setChecked(device.isOn());
            mCheckBox.setEnabled(device.isWritable());
        }

    }

    private static class UnknownViewHolder extends DeviceViewHolder {

        UnknownViewHolder(View view) {
            super(view);
        }

        void setDevice(Device device) {
        }

    }

    private static class ContactViewHolder extends DeviceViewHolder {

        private RadioButton mRadioButton;

        ContactViewHolder(View view) {
            super(view);

            mRadioButton = (RadioButton) view.findViewById(R.id.radiobutton);
        }

        @Override
        void setDevice(Device device) {
            super.setDevice(device);

            mRadioButton.setChecked(device.isClosed());
        }
    }

    private static class ScreenViewHolder extends DeviceViewHolder {

        private final ImageButton mUpAction;

        private final ImageButton mDownAction;

        ScreenViewHolder(View view) {
            super(view);

            mUpAction = (ImageButton) view.findViewById(R.id.up_action);
            mDownAction = (ImageButton) view.findViewById(R.id.down_action);

            mUpAction.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    getDevice().setValue(Device.VALUE_UP);
                    getDeviceChangeListener().onDeviceChange(getDevice(), Device.Properties.VALUE.ordinal());
                }
            });

            mDownAction.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    getDevice().setValue(Device.VALUE_DOWN);
                    getDeviceChangeListener().onDeviceChange(getDevice(), Device.Properties.VALUE.ordinal());
                }
            });
        }

        @Override
        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        void setDevice(Device device) {
            super.setDevice(device);

            mUpAction.setEnabled(device.isWritable());
            mDownAction.setEnabled(device.isWritable());

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                mUpAction.setAlpha(device.isWritable() ? 1.0f : .5f);
                mDownAction.setAlpha(device.isWritable() ? 1.0f : .5f);
            }
        }
    }

    private static class DimmerViewHolder extends SwitchViewHolder {

        private SeekBar mSeekBar;

        DimmerViewHolder(View view) {
            super(view);
            mSeekBar = (SeekBar) view.findViewById(R.id.seekbar);
            mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean b) {}

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    getDevice().setDimLevel(seekBar.getProgress());
                    getDeviceChangeListener().onDeviceChange(getDevice(), Device.Properties.DIMLEVEL.ordinal());
                }
            });
        }

        void setDevice(Device device) {
            super.setDevice(device);
            mSeekBar.setProgress(device.getDimLevel());
            mSeekBar.setEnabled(device.isWritable());
        }

    }

    private static class WeatherViewHolder extends DeviceViewHolder {

        private final ViewGroup mTemperature;
        private final TextView mTemperatureText;
        private final ViewGroup mHumidity;
        private final TextView mHumidityText;
        private final ViewGroup mSunrise;
        private final TextView mSunriseText;
        private final ViewGroup mSunset;
        private final TextView mSunsetText;
        private final ViewGroup mBattery;
        private final ImageView mBatteryImage;
        private final ViewGroup mUpdate;
        private final ImageButton mUpdateBtn;

        private long mTimestamp;

        private final DecimalFormat temperatureFormat = new DecimalFormat("#°");
        private final DecimalFormat humidityFormat = new DecimalFormat("#%");
        private final DecimalFormat timeFormat = new DecimalFormat("00");

        private static Drawable sBatteryFullDrawable;
        private static Drawable sBatteryEmptyDrawable;

        WeatherViewHolder(View view) {
            super(view);

            mTemperature = (ViewGroup) view.findViewById(R.id.temperature);
            mTemperatureText = (TextView) view.findViewById(R.id.temperature_text);

            mHumidity = (ViewGroup) view.findViewById(R.id.humidity);
            mHumidityText = (TextView) view.findViewById(R.id.humidity_text);

            mBattery = (ViewGroup) view.findViewById(R.id.battery);
            mBatteryImage = (ImageView) view.findViewById(R.id.battery_image);

            mSunrise = (ViewGroup) view.findViewById(R.id.sunrise);
            mSunriseText = (TextView) view.findViewById(R.id.sunrise_text);

            mSunset = (ViewGroup) view.findViewById(R.id.sunset);
            mSunsetText = (TextView) view.findViewById(R.id.sunset_text);

            mUpdate = (ViewGroup) view.findViewById(R.id.update);
            mUpdateBtn = (ImageButton) view.findViewById(R.id.update_btn);

            Calendar calender = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            mTimestamp = calender.getTimeInMillis()/1000;
        }

        static void setBatteryDrawables(Drawable full, Drawable empty) {
            sBatteryEmptyDrawable = empty;
            sBatteryFullDrawable = full;
        }

        void setDevice(Device device) {
            super.setDevice(device);

            if (device.hasTemperatureValue() && device.isShowTemperature()) {
                mTemperature.setVisibility(View.VISIBLE);
                mTemperatureText.setText(temperatureFormat.format(
                        (device.getTemperature() / Math.pow(10, device.getDeviceDecimals()))));
            } else {
                mTemperature.setVisibility(View.GONE);
            }

            if (device.hasSunriseValue() && device.hasSunsetValue() && device.isShowSunriseset()) {
                mSunrise.setVisibility(View.VISIBLE);
                mSunset.setVisibility(View.VISIBLE);
                int hour = device.getSunrise() / 100;
                int min = (device.getSunrise() - (hour*100));
                mSunriseText.setText(timeFormat.format(Double.valueOf(hour)) + ":" + timeFormat.format(Double.valueOf(min)));
                hour = device.getSunset() / 100;
                min = (device.getSunset() - (hour*100));
                mSunsetText.setText(timeFormat.format(Double.valueOf(hour)) + ":" + timeFormat.format(Double.valueOf(min)));
            } else {
                mSunrise.setVisibility(View.GONE);
                mSunset.setVisibility(View.GONE);
            }

            if (device.hasHumidityValue() && device.isShowHumidity()) {
                mHumidity.setVisibility(View.VISIBLE);
                mHumidityText.setText(humidityFormat.format(
                        (device.getHumidity() / Math.pow(10, device.getDeviceDecimals()) / 100)));
            } else {
                mHumidity.setVisibility(View.GONE);
            }

            if (device.hasBatteryValue() && device.isShowBattery()) {
                mBattery.setVisibility(View.VISIBLE);
                mBatteryImage.setImageDrawable(device.hasHealthyBattery()
                        ? sBatteryFullDrawable : sBatteryEmptyDrawable);
            } else {
                mBattery.setVisibility(View.GONE);
            }

            if (device.isShowUpdate()) {
                mUpdate.setVisibility(View.VISIBLE);
                mUpdateBtn.setOnClickListener((new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (mUpdateBtn.isEnabled()) {
                            getDeviceChangeListener().onDeviceChange(getDevice(), Device.Properties.UPDATE.ordinal());
                            mUpdateBtn.setEnabled(false);
                            if(Build.VERSION.SDK_INT >= 11) {
                                mUpdateBtn.setAlpha((float) 0.3);
                            } else {
                                AlphaAnimation alpha = new AlphaAnimation(0.3F, 0.3F);
                                alpha.setDuration(0);
                                alpha.setFillAfter(true);
                                mUpdateBtn.startAnimation(alpha);
                            }
                        }
                    }
                }));
                if((mTimestamp-device.getTimestamp()) <= device.getMinInterval()) {
                    if(mUpdateBtn.isEnabled()) {
                        mUpdateBtn.setEnabled(false);
                        if(Build.VERSION.SDK_INT >= 11) {
                            mUpdateBtn.setAlpha((float) 0.3);
                        } else {
                            AlphaAnimation alpha = new AlphaAnimation(0.3F, 0.3F);
                            alpha.setDuration(0);
                            alpha.setFillAfter(true);
                            mUpdateBtn.startAnimation(alpha);
                        }
                    }
                }

            } else {
                mUpdate.setVisibility(View.GONE);
            }
        }

    }

}
