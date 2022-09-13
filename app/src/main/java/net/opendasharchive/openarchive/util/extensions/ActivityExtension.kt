package net.opendasharchive.openarchive.util.extensions

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity

fun FragmentActivity.routeTo(fragment: Fragment, containerId: Int) {
    supportFragmentManager.beginTransaction()
            .replace(containerId, fragment)
            .addToBackStack(fragment::class.java.canonicalName)
            .commit()
}