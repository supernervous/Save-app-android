package net.opendasharchive.openarchive.features.onboarding23

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.core.content.res.ResourcesCompat
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import net.opendasharchive.openarchive.databinding.FragmentOnboarding23SlideBinding

private const val ARG_TITLE = "title_param"
private const val ARG_SUMMARY = "summary_param"

class Onboarding23SlideFragment : Fragment() {
    private var title: String? = null
    private var summary: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            title = it.getString(ARG_TITLE)
            summary = it.getString(ARG_SUMMARY)
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val mBinding = FragmentOnboarding23SlideBinding.inflate(inflater)
        mBinding.title.text = title
        mBinding.summary.text = HtmlCompat.fromHtml("${this.summary}", HtmlCompat.FROM_HTML_MODE_COMPACT)
        return mBinding.root
        // return inflater.inflate(R.layout.fragment_onboarding23_slide, container, false)
    }

    companion object {
        @JvmStatic
        fun newInstance(context: Context, @StringRes title: Int, @StringRes summary: Int) =
            Onboarding23SlideFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TITLE, context.getString(title))
                    putString(ARG_SUMMARY, context.getString(summary))
                }
            }
    }
}