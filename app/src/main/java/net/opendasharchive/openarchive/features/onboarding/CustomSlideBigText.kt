package net.opendasharchive.openarchive.features.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import net.opendasharchive.openarchive.databinding.CustomSlideBigTextBinding

class CustomSlideBigText : Fragment() {

    private var mTitle = 0
    private var mSubTitle = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mTitle = arguments?.getInt(ARG_LAYOUT_TITLE) ?: 0
        mSubTitle = arguments?.getInt(ARG_LAYOUT_SUBTITLE) ?: 0
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = CustomSlideBigTextBinding.inflate(inflater, container, false)

        binding.customSlideBigText.setText(mTitle)

        if (mSubTitle != 0) {
            binding.customSlideBigTextSub.setText(mSubTitle)
            binding.customSlideBigTextSub.visibility = View.VISIBLE
        }

        return binding.root
    }

    companion object {

        private const val ARG_LAYOUT_TITLE = "layoutTitle"
        private const val ARG_LAYOUT_SUBTITLE = "layoutSubtitle"

        fun newInstance(title: Int, subtitle: Int): CustomSlideBigText = CustomSlideBigText().also {
            it.arguments = Bundle().apply {
                putInt(ARG_LAYOUT_TITLE, title)
                putInt(ARG_LAYOUT_SUBTITLE, subtitle)
            }
        }
    }
}