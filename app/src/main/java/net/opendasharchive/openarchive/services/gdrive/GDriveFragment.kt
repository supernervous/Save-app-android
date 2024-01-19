package net.opendasharchive.openarchive.services.gdrive

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.CleanInsightsManager
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.FragmentGdriveBinding
import net.opendasharchive.openarchive.db.Space

class GDriveFragment : Fragment() {

    private lateinit var mBinding: FragmentGdriveBinding

    companion object {
        const val RESP_CANCEL = "gdrive_fragment_resp_cancel"
        const val RESP_AUTHENTICATED = "gdrive_fragment_resp_authenticated"

        const val REQUEST_CODE_GOOGLE_AUTH = 21701
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mBinding = FragmentGdriveBinding.inflate(inflater)

        mBinding.error.visibility = View.GONE

        mBinding.btBack.setOnClickListener {
            setFragmentResult(RESP_CANCEL, bundleOf())
        }

        mBinding.btAuthenticate.setOnClickListener {
            mBinding.error.visibility = View.GONE
            authenticate()
            mBinding.btBack.isEnabled = false
            mBinding.btAuthenticate.isEnabled = false
        }

        return mBinding.root
    }

    private fun authenticate() {
        if (!GDriveConduit.permissionsGranted(requireContext())) {
            GoogleSignIn.requestPermissions(
                requireActivity(),
                REQUEST_CODE_GOOGLE_AUTH,
                GoogleSignIn.getLastSignedInAccount(requireActivity()),
                *GDriveConduit.SCOPES
            )
        } else {
            // permission was already granted, we're already signed in, continue.
            setFragmentResult(RESP_AUTHENTICATED, bundleOf())
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_GOOGLE_AUTH) {
            when (resultCode) {
                RESULT_OK -> {
                    CoroutineScope(Dispatchers.IO).launch {
                        val space = Space(Space.Type.GDRIVE)
                        // we don't really know the host here, that's hidden by Drive Api
                        space.host = "what's the host of google drive? :shurg:"
                        data?.let { it ->
                            val result = Auth.GoogleSignInApi.getSignInResultFromIntent(it)
                            if (result?.isSuccess == true) {
                                result.signInAccount?.let { account ->
                                    space.displayname = account.email ?: ""
                                }
                            }
                        }
                        space.save()
                        Space.current = space

                        CleanInsightsManager.getConsent(requireActivity()) {
                            CleanInsightsManager.measureEvent(
                                "backend",
                                "new",
                                Space.Type.GDRIVE.friendlyName
                            )
                        }

                        MainScope().launch {
                            setFragmentResult(RESP_AUTHENTICATED, bundleOf())
                        }
                    }
                }

                else -> {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.gdrive_authentication_canceled_message),
                        Toast.LENGTH_LONG
                    ).show()
                    mBinding.btBack.isEnabled = true
                    mBinding.btAuthenticate.isEnabled = true
                }
            }
        }
    }
}