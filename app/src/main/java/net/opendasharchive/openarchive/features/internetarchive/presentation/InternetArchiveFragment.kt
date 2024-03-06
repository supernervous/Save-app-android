package net.opendasharchive.openarchive.features.internetarchive.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.internetarchive.presentation.login.ARG_SPACE
import net.opendasharchive.openarchive.features.internetarchive.presentation.login.ARG_VAL_NEW_SPACE
import net.opendasharchive.openarchive.features.internetarchive.presentation.login.InternetArchiveLoginScreen
import net.opendasharchive.openarchive.features.internetarchive.presentation.login.getSpace

const val RESP_SAVED = "ia_fragment_resp_saved"
const val RESP_DELETED = "ia_dav_fragment_resp_deleted"
const val RESP_CANCEL = "ia_fragment_resp_cancel"

@Deprecated("only used for backward compatibility")
class InternetArchiveFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val (space, isNewSpace) = arguments.getSpace(Space.Type.INTERNET_ARCHIVE)

        return ComposeView(requireContext()).apply {
            setContent {
                if (isNewSpace) {
                    InternetArchiveLoginScreen(space) { result ->
                        setFragmentResult(result, bundleOf())
                    }
                } else {
                    InternetArchiveScreen(space) { result ->
                        setFragmentResult(result, bundleOf())
                    }
                }
            }
        }
    }

    companion object {

        const val RESP_SAVED = "ia_fragment_resp_saved"
        const val RESP_DELETED = "ia_dav_fragment_resp_deleted"
        const val RESP_CANCEL = "ia_fragment_resp_cancel"

        @JvmStatic
        fun newInstance(spaceId: Long) = InternetArchiveFragment().apply {
            arguments = Bundle().apply {
                putLong(ARG_SPACE, spaceId)
            }
        }

        @JvmStatic
        fun newInstance() = newInstance(ARG_VAL_NEW_SPACE)
    }
}
