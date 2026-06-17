package com.turkcell.bip.navigation

import android.app.Activity
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

fun NavBackStack<out NavKey>.popOrFinish(activity: Activity?) {
    if (size > 1) removeLastOrNull() else activity?.finish()
}
