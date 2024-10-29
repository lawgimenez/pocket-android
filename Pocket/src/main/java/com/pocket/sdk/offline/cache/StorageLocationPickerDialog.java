package com.pocket.sdk.offline.cache;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.ideashower.readitlater.R;
import com.pocket.app.App;
import com.pocket.sdk.Pocket;
import com.pocket.sdk.api.generated.enums.CxtEvent;
import com.pocket.sdk.api.generated.enums.CxtSection;
import com.pocket.sdk.api.generated.enums.CxtView;
import com.pocket.sdk.util.PermissionRequester;
import com.pocket.sdk.util.dialog.ProgressDialogFragment;
import com.pocket.sdk.util.file.AndroidStorageLocation;
import com.pocket.sdk.util.file.AndroidStorageUtil;
import com.pocket.sdk.util.file.RemovableAndroidStorage;
import com.pocket.sdk2.analytics.context.Interaction;
import com.pocket.ui.view.menu.RadioButton;
import com.pocket.util.android.FormFactor;
import com.pocket.util.android.ViewUtilKt;
import com.pocket.util.android.text.BulletTextUtil;
import com.pocket.util.java.BytesUtil;
import com.pocket.util.java.SimpleCallback;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO Documentation
 */
public abstract class StorageLocationPickerDialog {

    public static void show(final Context context, SimpleCallback retry) {
        final Adapter adapter = new Adapter(context);
        new AlertDialog.Builder(context)
                .setTitle(R.string.setting_storage_location)
                .setAdapter(adapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final AndroidStorageLocation location = adapter.getItem(which).location;

                        if (location.isAvailable()) {
                            if (adapter.mSelected == location) {
                                return; // Already selected
                            }

                            new AlertDialog.Builder(context)
                                    .setTitle(R.string.dg_confirm_t)
                                    .setMessage(R.string.dg_sdcard_confirm_change_m)
                                    .setNegativeButton(R.string.ac_cancel, null)
                                    .setPositiveButton(R.string.ac_yes, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            try {
                                                App.from(context).assets().setStorageLocation(location);
                                                final ProgressDialogFragment progress = ProgressDialogFragment.getNew(R.string.dg_changing_data_location, false);
                                                progress.showOnCurrentActivity();
                                                App.from(context).assets().clearOfflineContent(null, progress::dismissAllowingStateLoss);
                                                
                                                trackResult(CxtEvent.SDCARD_SETUP_SUCCESS, context);
                                                
                                            } catch (AssetDirectoryUnavailableException e) {
                                                new AlertDialog.Builder(context)
                                                        .setTitle(R.string.dg_storage_setting_unavailable_t)
                                                        .setMessage(R.string.dg_storage_setting_unavailable_m)
                                                        .setPositiveButton(R.string.ac_ok, null)
                                                        .show();
                                                trackResult(CxtEvent.SDCARD_SETUP_MISSING, context);
                                            }
                                        }
                                    })
                                    .show();

                        } else {
                            switch (location.checkPermissions()) {
                                case NOT_GRANTED:
                                    new AlertDialog.Builder(context)
                                            .setTitle(R.string.dg_storage_setting_unavailable_t)
                                            .setMessage(R.string.dg_offline_cache_is_missing_permission_m)
                                            .setPositiveButton(R.string.ac_ok,
                                                    (dialog1, which1) ->
                                                            new PermissionRequester(context, 42, (allGranted, permissions, results) -> {
                                                                if (allGranted) retry.callback();
                                                            }, Manifest.permission.WRITE_EXTERNAL_STORAGE).request())
                                            .show();
                                    trackResult(CxtEvent.SDCARD_SETUP_MISSING, context);
                                    break;
                                
                                case GRANTED_BUT_FILE_SYSTEM_DENIED:
                                    // Seems like the weird issue where some devices don't give us permission properly.
                                    new AlertDialog.Builder(context)
                                            .setTitle(R.string.dg_storage_setting_unavailable_t)
                                            .setMessage(R.string.dg_offline_cache_is_missing_rw_permission_m)
                                            .setPositiveButton(R.string.ac_ok, null)
                                            .show();
                                    trackResult(CxtEvent.SDCARD_SETUP_DENIED, context);
                                    break;
                                
                                case GRANTED:
                                case UNAVAILABLE:
                                default:
                                    new AlertDialog.Builder(context)
                                            .setTitle(R.string.dg_storage_setting_unavailable_t)
                                            .setMessage(R.string.dg_storage_setting_unavailable_m)
                                            .setPositiveButton(R.string.ac_ok, null)
                                            .show();
                                    trackResult(CxtEvent.SDCARD_SETUP_FS_DENIED, context);
                                    break;
                            }
                        }
                    }
                })
                .show();
    }
    
    private static void trackResult(CxtEvent event, Context context) {
        Interaction i = Interaction.on(context);
        Pocket pocket = App.from(context).pocket();
        pocket.sync(null, pocket.spec().actions().pv()
                .time(i.time)
                .context(i.context)
                .section(CxtSection.SETTINGS)
                .view(CxtView.MENU)
                .event(event)
                .version("1")
                .event_type(9)
                .build());
    }

    private static class Adapter extends BaseAdapter {

        private final Context mContext;
        private final ArrayList<Option> mOptions = new ArrayList<>();
        private AndroidStorageLocation mSelected = null;

        private Adapter(Context context) {
            mContext = context;

            int internalName;
            int externalName;
            boolean showExternal;
            boolean showExternalPath;
            AndroidStorageUtil.ExternalType type = AndroidStorageUtil.getExternalType(context);
            switch (type) {
                case INTERNAL_ONLY:
                    showExternal = false;
                    internalName = R.string.storage_type_internal;
                    externalName = R.string.storage_type_removable;
                    showExternalPath = false;
                    break;
                case EXTERNAL_AS_REMOVABLE:
                    showExternal = true;
                    internalName = R.string.storage_type_internal;
                    externalName = R.string.storage_type_removable;
                    showExternalPath = true;
                    break;
                case EXTERNAL_AS_PUBLIC_INTERNAL:
                    showExternal = true;
                    internalName = R.string.storage_type_internal_specific_sandboxed;
                    externalName = R.string.storage_type_internal_specific_unsandboxed;
                    showExternalPath = false;
                    break;
                default:
                    throw new RuntimeException("unknown type " + type);
            }

            addIfAvailable(AndroidStorageUtil.getInternal(context), internalName, false, R.array.storage_location_specs_internal);

            if (showExternal) {
                addIfAvailable(AndroidStorageUtil.getExternal(context), externalName, showExternalPath, R.array.storage_location_specs_removable);
            }

            List<RemovableAndroidStorage> removables = AndroidStorageUtil.getRemovable(context);
            for (RemovableAndroidStorage location : removables) {
                addIfAvailable(location, R.string.storage_type_removable, true, R.array.storage_location_specs_removable);
            }

            try {
                AndroidStorageLocation selected = App.from(context).assets().getAssetDirectory().getStorageLocation();
                for (Option option : mOptions) {
                    if (option.location.equalsIncludingPath(selected)) {
                        mSelected = option.location;
                    }
                }
                if (mSelected == null) {
                    // The selected one isn't available to show.
                }
            } catch (AssetDirectoryUnavailableException e) {
                // Selected option isn't available, so just don't have anything selected.
                mSelected = null;
            }
        }

        private void addIfAvailable(AndroidStorageLocation location, int labelRes, boolean showPath, int bulletsStringArrayRes) {
            try {
                mOptions.add(new Option(location, labelRes, showPath, bulletsStringArrayRes));
            } catch (AssetDirectoryUnavailableException e) {
                // Option is unavailable, don't show
            }
        }

        @Override
        public int getCount() {
            return mOptions.size();
        }

        @Override
        public Option getItem(int position) {
            return mOptions.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewWrapper wrapper;
            View view;

            if (convertView != null) {
                view = convertView;
                wrapper = (ViewWrapper) view.getTag();
            } else {
                view = LayoutInflater.from(mContext).inflate(R.layout.view_storage_location_option, parent, false);
                wrapper = new ViewWrapper(view);
                view.setTag(wrapper);
            }

            Option option = getItem(position);
            wrapper.bind(option, option.location == mSelected);

            return view;
        }
    }

    private static class ViewWrapper {

        private final TextView mLabel;
        private final TextView mSubLabel;
        private final TextView mBullets;
        private final RadioButton mRadioButton;

        private ViewWrapper(View root) {
            mLabel = (TextView) root.findViewById(R.id.label);
            mSubLabel = (TextView) root.findViewById(R.id.sub_label);
            mBullets = (TextView) root.findViewById(R.id.bullets);
            mRadioButton = (RadioButton) root.findViewById(R.id.radio);

            // Clicks are handled by the list view
            mRadioButton.setFocusable(false);
            mRadioButton.setClickable(false);
        }

        public void bind(Option option, boolean isSelected) {
            mLabel.setText(option.label);
            ViewUtilKt.setTextOrHide(mSubLabel, option.sublabel);
            mBullets.setText(option.bullets);
            mRadioButton.setChecked(isSelected);
        }

    }

    private static class Option {

        public final AndroidStorageLocation location;
        public final CharSequence label;
        public final CharSequence sublabel;
        public final CharSequence bullets;

        private Option(AndroidStorageLocation location, int labelRes, boolean showPath, int bulletsStringArrayRes) throws AssetDirectoryUnavailableException {
            this(location, App.getStringResource(labelRes), showPath, bulletsStringArrayRes);
        }

        private Option(AndroidStorageLocation location, String label, boolean showPath, int bulletsStringArrayRes) throws AssetDirectoryUnavailableException {
            this.location = location;
            this.label = label;
            this.sublabel = showPath ? location.getPathOfParentOfAppDirectory(App.getContext()) : null;
            this.bullets = buildSpecList(bulletsStringArrayRes);
        }

        private CharSequence buildSpecList(int bulletsStringArrayRes) {
            CharSequence[] specific = App.getContext().getResources().getTextArray(bulletsStringArrayRes);
            CharSequence[] bullets = new CharSequence[specific.length + 1]; // +1 for Free space bullet

            int i = 0;

            for (CharSequence bullet : specific) {
                bullets[i++] = bullet;
            }

            String freeSpace;
            try {
                freeSpace = BytesUtil.bytesToCleanString(App.getContext(), location.getFreeSpaceBytes());
            } catch (Throwable t) {
                freeSpace = App.getContext().getString(R.string.storage_free_space_unknown);
            }
            bullets[i++] = App.getContext().getString(R.string.storage_free_space, freeSpace);

            return BulletTextUtil.makeBulletList(FormFactor.dpToPx(5), bullets);
        }

    }

}
