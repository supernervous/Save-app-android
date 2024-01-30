package net.opendasharchive.openarchive.services.internetarchive

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import net.opendasharchive.openarchive.databinding.FragmentIaLearnHowStepBinding

class IaLearnHowStepFragment : Fragment() {

    private lateinit var mBinding: FragmentIaLearnHowStepBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mBinding = FragmentIaLearnHowStepBinding.inflate(inflater)

        arguments?.let {
            mBinding.summary.text = getString(it.getInt(ARG_SUMMARY_STRING_RES))
            mBinding.illustration.setImageResource(it.getInt(ARG_ILLUSTRATION_DRAWABLE_RES))
        }

        return mBinding.root
    }

    companion object {
        const val ARG_SUMMARY_STRING_RES = "summary"
        const val ARG_ILLUSTRATION_DRAWABLE_RES = "illustration"

        fun newInstance(@StringRes summary: Int, @DrawableRes illustration: Int): IaLearnHowStepFragment {
            return IaLearnHowStepFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_SUMMARY_STRING_RES, summary)
                    putInt(ARG_ILLUSTRATION_DRAWABLE_RES, illustration)
                }
            }
        }
    }
}