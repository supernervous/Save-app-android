package net.opendasharchive.openarchive.features.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import net.opendasharchive.openarchive.databinding.CustomOnboardingMainBinding
import net.opendasharchive.openarchive.util.extensions.show

class CustomOnboardingScreen : Fragment() {

    private var mTitle = 0
    private var mSubTitle = 0
    private var mImageResource = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mTitle = arguments?.getInt(ARG_LAYOUT_TITLE) ?: 0
        mSubTitle = arguments?.getInt(ARG_LAYOUT_SUBTITLE) ?: 0
        mImageResource = arguments?.getInt(ARG_LAYOUT_IMAGE) ?: 0
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = CustomOnboardingMainBinding.inflate(inflater, container, false)

        binding.customSlideBigText.setText(mTitle)

        if (mSubTitle != 0) {
            binding.customSlideBigTextSub.setText(mSubTitle)
            binding.customSlideBigTextSub.show()
        }

        binding.customSlideImage.setImageResource(mImageResource)

        return binding.root
    }

    companion object {

        private const val ARG_LAYOUT_TITLE = "layoutTitle"
        private const val ARG_LAYOUT_SUBTITLE = "layoutSubtitle"
        private const val ARG_LAYOUT_IMAGE = "layoutImage"

        fun newInstance(title: Int, subtitle: Int, image: Int): CustomOnboardingScreen
        = CustomOnboardingScreen().also {
            it.arguments = Bundle().apply {
                putInt(ARG_LAYOUT_TITLE, title)
                putInt(ARG_LAYOUT_SUBTITLE, subtitle)
                putInt(ARG_LAYOUT_IMAGE, image)
            }
        }
    }
}