package net.opendasharchive.openarchive.features.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.FragmentSpaceSetupBinding
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.util.extensions.Position
import net.opendasharchive.openarchive.util.extensions.hide
import net.opendasharchive.openarchive.util.extensions.setDrawable
import net.opendasharchive.openarchive.util.extensions.tint

class SpaceSetupFragment : Fragment() {

    private lateinit var mBinding: FragmentSpaceSetupBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mBinding = FragmentSpaceSetupBinding.inflate(inflater)

        val color = ContextCompat.getColor(requireContext(), R.color.colorPrimary)
        val arrow = ContextCompat.getDrawable(requireContext(), R.drawable.ic_arrow_right)
        arrow?.tint(color)

        mBinding.webdavIcon.setColorFilter(color)

        mBinding.webdavText.setDrawable(arrow, Position.End, tint = false)
        mBinding.dropboxText.setDrawable(arrow, Position.End, tint = false)
        mBinding.internetArchiveText.setDrawable(arrow, Position.End, tint = false)

        mBinding.webdav.setOnClickListener {
            setFragmentResult(RESULT_REQUEST_KEY, bundleOf(RESULT_BUNDLE_KEY to RESULT_VAL_WEBDAV))
        }

        if (Space.has(Space.Type.DROPBOX)) {
            mBinding.dropbox.hide()
        } else {
            mBinding.dropbox.setOnClickListener {
                setFragmentResult(
                    RESULT_REQUEST_KEY,
                    bundleOf(RESULT_BUNDLE_KEY to RESULT_VAL_DROPBOX)
                )
            }
        }

        if (Space.has(Space.Type.INTERNET_ARCHIVE)) {
            mBinding.internetArchive.hide()
        } else {
            mBinding.internetArchive.setOnClickListener {
                setFragmentResult(
                    RESULT_REQUEST_KEY,
                    bundleOf(RESULT_BUNDLE_KEY to RESULT_VAL_INTERNET_ARCHIVE)
                )
            }
        }

        return mBinding.root
    }

    companion object {
        const val RESULT_REQUEST_KEY = "space_setup_fragment_result"
        const val RESULT_BUNDLE_KEY = "space_setup_result_key"
        const val RESULT_VAL_DROPBOX = "dropbox"
        const val RESULT_VAL_WEBDAV = "webdav"
        const val RESULT_VAL_INTERNET_ARCHIVE = "internet_archive"
    }
}