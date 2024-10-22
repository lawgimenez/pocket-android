package utils.pocket

import com.android.build.gradle.internal.dsl.BaseAppModuleExtension

fun BaseAppModuleExtension.setupVariantFilters() {
    variantFilter {
        val type = buildType.name
        val flavor = flavors.first().name

        if ((type == BuildTypes.DEBUG &&
                    flavor != Flavors.DEVELOP
                    ) ||
            (type == BuildTypes.TEAM_RELEASE &&
                    flavor != Flavors.TEAM_A &&
                    flavor != Flavors.TEAM_REVIEW &&
                    flavor != Flavors.PREMIUM_REVIEW
                    ) ||
            (type == BuildTypes.UNSIGNED_RELEASE &&
                    flavor != Flavors.PLAY
                    )
        ) {
            ignore = true
        }
    }
}