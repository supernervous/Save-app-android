package net.opendasharchive.openarchive.services.dropbox

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import com.dropbox.core.android.Auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.CleanInsightsManager
import net.opendasharchive.openarchive.databinding.FragmentDropboxBinding
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.services.SaveClient

class DropboxFragment : Fragment() {

    private lateinit var mBinding: FragmentDropboxBinding
    private var mAwaitingAuth = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mBinding = FragmentDropboxBinding.inflate(inflater)

        mBinding.error.visibility = View.GONE

        mBinding.btBack.setOnClickListener {
            // finish()
            setFragmentResult(RESP_CANCEL, bundleOf())
        }

        mBinding.btAuthenticate.setOnClickListener {
            mBinding.error.visibility = View.GONE
            authenticate()
        }

        return mBinding.root
    }

    override fun onResume() {
        super.onResume()

        if (!mAwaitingAuth) return

        val accessToken = Auth.getOAuth2Token() ?: return

        mAwaitingAuth = false

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val client = SaveClient.getDropbox(requireContext(), accessToken)

                val username = try {
                    client.users()?.currentAccount?.email ?: Auth.getUid()
                } catch (e: Exception) {
                    Auth.getUid()
                }

                val space = Space(Space.Type.DROPBOX)
                username?.let { space.username = it }
                space.password = accessToken
                space.save()
                Space.current = space

                CleanInsightsManager.getConsent(requireActivity()) {
                    CleanInsightsManager.measureEvent("backend", "new", Space.Type.DROPBOX.friendlyName)
                }

                MainScope().launch {
                    setFragmentResult(RESP_AUTHENTICATED, bundleOf())
                }
            } catch (e: Exception) {
                MainScope().launch {
                    mBinding.error.text = e.localizedMessage
                    mBinding.error.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun authenticate() {
        Auth.startOAuth2Authentication(requireContext(), "gd5sputfo57s1l1")

        mAwaitingAuth = true
    }

    companion object {
        const val RESP_CANCEL = "dropbox_fragment_resp_cancel"
        const val RESP_AUTHENTICATED = "dropbox_fragment_resp_authenticated"
    }
}