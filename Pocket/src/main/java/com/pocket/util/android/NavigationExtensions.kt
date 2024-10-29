package com.pocket.util.android

import androidx.navigation.NavController
import androidx.navigation.NavDirections

fun NavController.navigateSafely(directions: NavDirections) =
    currentDestination?.getAction(directions.actionId)?.run { navigate(directions) }