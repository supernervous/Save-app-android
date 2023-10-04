package net.opendasharchive.openarchive.features.settings

import net.opendasharchive.openarchive.databinding.ContentCcBinding
import net.opendasharchive.openarchive.util.extensions.openBrowser
import net.opendasharchive.openarchive.util.extensions.toggle

object CcSelector {

    private const val CC_DOMAIN = "creativecommons.org"
    private const val CC_URL = "https://%s/licenses/%s/4.0/"

    fun init(cc: ContentCcBinding, license: String?, enabled: Boolean = true, update: (license: String?) -> Unit) {
        set(cc, license, enabled)

        cc.swCc.setOnCheckedChangeListener { _, isChecked ->
            toggle(cc, isChecked)

            update(get(cc))
        }

        cc.swNd.setOnCheckedChangeListener { _, isChecked ->
            cc.swSa.isEnabled = isChecked

            update(get(cc))
        }

        cc.swSa.setOnCheckedChangeListener { _, _ -> update(get(cc)) }
        cc.swNc.setOnCheckedChangeListener { _, _ -> update(get(cc)) }

        cc.btLearnMore.setOnClickListener {
            it?.context?.openBrowser("https://creativecommons.org/about/cclicenses/")
        }
    }

    fun set(cc: ContentCcBinding, license: String?, enabled: Boolean = true) {
        val isCc = license?.contains(CC_DOMAIN, true) ?: false

        cc.swCc.isChecked = isCc
        toggle(cc, isCc)

        cc.swNd.isChecked = isCc && !(license?.contains("-nd", true) ?: false)
        cc.swSa.isEnabled = cc.swNd.isChecked
        cc.swSa.isChecked = isCc && cc.swNd.isChecked && license?.contains("-sa", true) ?: false
        cc.swNc.isChecked = isCc && !(license?.contains("-nc", true) ?: false)

        cc.tvLicense.text = license

        cc.swCc.isEnabled = enabled
        cc.swNd.isEnabled = enabled
        cc.swSa.isEnabled = enabled
        cc.swNc.isEnabled = enabled
    }

    fun get(cc: ContentCcBinding): String? {
        var license: String? = null

        if (cc.swCc.isChecked) {
            license = "by"

            if (cc.swNd.isChecked) {
                if (!cc.swNc.isChecked) {
                    license += "-nc"
                }

                if (cc.swSa.isChecked) {
                    license += "-sa"
                }
            }
            else {
                cc.swSa.isChecked = false

                if (!cc.swNc.isChecked) {
                    license += "-nc"
                }

                license += "-nd"
            }
        }

        if (license != null) {
            license = String.format(CC_URL, CC_DOMAIN, license)
        }

        cc.tvLicense.text = license

        return license
    }

    private fun toggle(cc: ContentCcBinding, value: Boolean) {
        cc.row1.toggle(value)
        cc.row2.toggle(value)
        cc.row3.toggle(value)
        cc.tvLicense.toggle(value)
    }
}