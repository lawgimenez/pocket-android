package com.pocket.app.settings.appicon

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.annotation.StringRes
import com.ideashower.readitlater.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AppIcons
@Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val automatic = AppIconOption(
        ComponentName(context, "com.ideashower.readitlater.activity.AppCacheCheckActivity"),
        label = R.string.setting_app_icon_classic,
        description = "Automatic",
    )

    val all = listOf(
        AppIconOption(
            ComponentName(context, "com.pocket.app.ClassicLogoActivity"),
            label = R.string.setting_app_icon_classic,
            description = "Classic",
        ),
        AppIconOption(
            ComponentName(context, "com.pocket.app.BlackAndWhiteLogoActivity"),
            label = R.string.setting_app_icon_monochrome,
            description = "Monochrome",
        ),
        AppIconOption(
            ComponentName(context, "com.pocket.app.PocketPrideActivity"),
            label = R.string.setting_app_icon_pride_flag,
            description = "Pride",
        )
    )

    var current: AppIconOption
        get() {
            return context.packageManager
                .resolveActivity(Intent(Intent.ACTION_MAIN).setPackage(context.packageName), 0)
                .let { resolveInfo ->
                    if (automatic.componentName.className == resolveInfo?.activityInfo?.name) {
                        automatic
                    } else {
                        all.first { it.componentName.className == resolveInfo?.activityInfo?.name }
                    }
                }
        }
        set(value) {
            val packageManager = context.packageManager
            packageManager.setComponentEnabledSetting(
                automatic,
                value.componentName.className == automatic.componentName.className,
            )
            for (option in all) {
                packageManager.setComponentEnabledSetting(
                    option,
                    value.componentName.className == option.componentName.className,
                )
            }
        }

    private fun PackageManager.setComponentEnabledSetting(
        option: AppIconOption,
        enabled: Boolean,
    ) {
        setComponentEnabledSetting(
            option.componentName,
            if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP,
        )
    }

    @SuppressLint("InlinedApi")
    fun loadIcon(option: AppIconOption): Drawable? {
        return context.packageManager.let {
            it.getActivityInfo(option.componentName, PackageManager.MATCH_DISABLED_COMPONENTS)
                .loadIcon(it)
        }
    }
}

data class AppIconOption(
    val componentName: ComponentName,
    @StringRes val label: Int,
    val description: String,
)
