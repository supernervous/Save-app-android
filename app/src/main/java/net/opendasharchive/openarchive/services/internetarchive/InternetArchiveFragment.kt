package net.opendasharchive.openarchive.services.internetarchive

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.CleanInsightsManager
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.FragmentInternetArchiveBinding
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.services.SaveClient
import net.opendasharchive.openarchive.util.AlertHelper
import net.opendasharchive.openarchive.util.Prefs
import net.opendasharchive.openarchive.util.extensions.Position
import net.opendasharchive.openarchive.util.extensions.makeSnackBar
import net.opendasharchive.openarchive.util.extensions.setDrawable
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import java.io.IOException
import java.io.InputStream
import kotlin.coroutines.suspendCoroutine

class InternetArchiveFragment : Fragment() {

    private lateinit var mSnackbar: Snackbar
    private lateinit var mSpace: Space
    private var mSpaceId: Long = ARG_VAL_NEW_SPACE
    private lateinit var mBinding: FragmentInternetArchiveBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mBinding = FragmentInternetArchiveBinding.inflate(inflater)

        mSpaceId = arguments?.getLong(ARG_SPACE) ?: ARG_VAL_NEW_SPACE

        if (ARG_VAL_NEW_SPACE != mSpaceId) {
            mSpace = Space.get(mSpaceId) ?: Space(Space.Type.INTERNET_ARCHIVE)

            mBinding.header.visibility = View.GONE

            mBinding.accessKey.isEnabled = false
            mBinding.secretKey.isEnabled = false

            mBinding.btAcquireKeys.isEnabled = false

            mBinding.btRemove.setDrawable(R.drawable.ic_delete, Position.Start, 0.5)
            mBinding.btRemove.visibility = View.VISIBLE
            mBinding.btRemove.setOnClickListener {
                removeProject()
            }

            mBinding.buttonBar.visibility = View.GONE
        }
        else {
            mSpace = Space(Space.Type.INTERNET_ARCHIVE)
        }

        mBinding.accessKey.setText(mSpace.username)
        mBinding.secretKey.setText(mSpace.password)

        mBinding.secretKey.setOnEditorActionListener { _: TextView?, id: Int, _: KeyEvent? ->
            if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                attemptLogin()
                return@setOnEditorActionListener true
            }
            false
        }

        mBinding.btAcquireKeys.setOnClickListener {
            acquireKeys()
        }

        mBinding.btLearnHow.setOnClickListener {
            Prefs.iaHintShown = false
            showFirstTimeIa()
        }

        mBinding.btBack.setOnClickListener {
            setFragmentResult(InternetArchiveFragment.RESP_CANCEL, bundleOf())
        }

        mBinding.btNext.setOnClickListener {
            attemptLogin()
        }

        showFirstTimeIa()

        return mBinding.root
    }

    private val mAcquireKeysResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode != AppCompatActivity.RESULT_OK) {
            return@registerForActivityResult
        }

        val username = it.data?.getStringExtra(IaScrapeActivity.EXTRAS_KEY_USERNAME)
        val credentials = it.data?.getStringExtra(IaScrapeActivity.EXTRAS_KEY_CREDENTIALS)

        mBinding.accessKey.setText(username)
        mBinding.secretKey.setText(credentials)
    }

    private fun acquireKeys() {
        mAcquireKeysResultLauncher.launch(Intent(requireContext(), IaScrapeActivity::class.java))
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private fun attemptLogin() {
        // Store values at the time of the login attempt.
        mSpace.username = mBinding.accessKey.text.toString()
        mSpace.password = mBinding.secretKey.text.toString()
        var focusView: View? = null

        // Check for a valid password, if the user entered one.
        if (mSpace.password.isEmpty()) {
            mBinding.secretKey.error = getString(R.string.error_field_required)
            focusView = mBinding.secretKey
        }

        // Check for a valid password, if the user entered one.
        if (mSpace.username.isEmpty()) {
            mBinding.accessKey.error = getString(R.string.error_field_required)
            focusView = mBinding.accessKey
        }

        if (focusView != null) {
            // There was an error; don't attempt login and focus the first form field with an error.
            focusView.requestFocus()
            Toast.makeText(requireContext(), getString(R.string.IA_login_error), Toast.LENGTH_SHORT).show()

            return
        }

        // Show a progress spinner, and kick off a background task to
        // perform the user login attempt.
        mSnackbar = mBinding.root.makeSnackBar(getString(R.string.login_activity_logging_message))
        mSnackbar?.show()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                testConnection()

                mSpace.save()

                Space.current = mSpace

                CleanInsightsManager.getConsent(requireActivity()) {
                    CleanInsightsManager.measureEvent("backend", "new", Space.Type.INTERNET_ARCHIVE.friendlyName)
                }

                mSnackbar.dismiss()

                setFragmentResult(RESP_SAVED, bundleOf())
            }
            catch (exception: IOException) {
                if (exception.message?.startsWith("401") == true) {
                    showError(getString(R.string.error_incorrect_username_or_password), true)
                } else {
                    showError(exception.localizedMessage ?: getString(R.string.error))
                }
            }
        }
    }

    /**
     * Unfortunately, this test actually only tests if the `access key` is correct.
     * We can provide any `secret key` to the IA's S3 API.
     *
     * I couldn't find a test which proofs the latter, too, short of `PUT`ing an asset on their
     * server. Which is a really bad idea, considering that we cannot `DELETE` the created bucket again.
     */
    private suspend fun testConnection() {
        val url = mSpace.hostUrl ?: throw IOException("400 Bad Request")

        val client = SaveClient.get(requireContext())

        val request = Request.Builder()
            .url(url)
            .method("GET", null)
            .addHeader("Authorization", "LOW ${mSpace.username}:${mSpace.password}")
            .build()

        return suspendCoroutine {
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    it.resumeWith(Result.failure(e))
                }

                override fun onResponse(call: Call, response: Response) {
                    val code = response.code
                    val message = response.message

                    val username = getUsername(response.body?.byteStream())

                    response.close()

                    if (code != 200 && code != 204) {
                        return it.resumeWith(Result.failure(IOException("$code $message")))
                    }

                    if (username == null) {
                        return it.resumeWith(Result.failure(IOException("401 Unauthorized")))
                    }

                    mSpace.displayname = username

                    it.resumeWith(Result.success(Unit))
                }
            })
        }
    }

    /**
     * Parses the usernome out of an XML document which starts like this:
     *
     * ```
     * <ListAllMyBucketsResult>
     *     <Owner>
     *         <ID>OpaqueIDStringGoesHere</ID>
     *         <DisplayName>Readable ID Goes Here</DisplayName>
     *     </Owner>
     * </<ListAllMyBucketsResult>
     * ```
     *
     * The username is expected in the first `DisplayName` tag in the first `Owner` tag in the
     * first `ListAllMyBucketsResult` tag.
     *
     * A username of `"Readable ID Goes Here".lowercase()` is considered not to be a username.
     * (That's what the Internet Archive S3 API should return, if authorization was unsuccessful.)
     */
    private fun getUsername(body: InputStream?): String? {
        if (body == null) return null

        try {
            val xpp = XmlPullParserFactory.newInstance().newPullParser()
            xpp.setInput(body, null)

            var eventType = xpp.eventType

            var container = false
            var owner = false
            var displayName = false

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (xpp.name) {
                            "ListAllMyBucketsResult" -> {
                                container = true
                            }
                            "Owner" -> {
                                if (container) owner = true
                            }
                            "DisplayName" -> {
                                if (container && owner) displayName = true
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        when (xpp.name) {
                            "ListAllMyBucketsResult" -> {
                                // Almost done anyway.
                                return null
                            }
                            "Owner" -> {
                                // It should be the first "Owner" element.
                                // If that went by without a "DisplayName" element, stop it.
                                if (container) return null
                            }
                            "DisplayName" -> {
                                // If the first "DisplayName" element in the first "Owner"
                                // element doesn't have a name, stop it.
                                if (container && owner) return null
                            }
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (container && owner && displayName) {
                            val username = xpp.text.trim()

                            // If the access key wasn't correct, a dummy username is displayed. Ignore.
                            if (username.isBlank() || username.lowercase() == "Readable ID Goes Here".lowercase()) {
                                return null
                            }

                            // Yay! Found a username!
                            return username
                        }
                    }
                }

                eventType = xpp.next()
            }
        }
        catch (e: XmlPullParserException) {
            // ignore
        }
        catch (e: IOException) {
            // ignore
        }

        return null
    }

    private fun showError(text: CharSequence, onForm: Boolean = false) {
        requireActivity().runOnUiThread {
            mSnackbar?.dismiss()

            if (onForm) {
                mBinding.secretKey.error = text
                mBinding.secretKey.requestFocus()
            }
            else {
                mSnackbar = mBinding.root.makeSnackBar(text, Snackbar.LENGTH_LONG)
                mSnackbar?.show()

                mBinding.accessKey.requestFocus()
            }
        }
    }

    private fun removeProject() {
        AlertHelper.show(requireContext(), R.string.are_you_sure_you_want_to_remove_this_server_from_the_app, R.string.remove_from_app, buttons = listOf(
            AlertHelper.positiveButton(R.string.remove) { _, _ ->
                mSpace.delete()
                setFragmentResult(RESP_DELETED, bundleOf())
            },
            AlertHelper.negativeButton()))
    }

    private fun showFirstTimeIa() {
        if (Prefs.iaHintShown) return

        val f = IaLearnHowFragment()
        f.show(requireActivity().supportFragmentManager, f.tag)

        Prefs.iaHintShown = true
    }

    companion object {
        const val ARG_SPACE = "space"
        const val ARG_VAL_NEW_SPACE = -1L

        const val RESP_SAVED = "ia_fragment_resp_saved"
        const val RESP_DELETED = "ia_dav_fragment_resp_deleted"
        const val RESP_CANCEL = "ia_fragment_resp_cancel"

        @JvmStatic
        fun newInstance(spaceId: Long) = InternetArchiveFragment().apply {
            arguments = Bundle().apply {
                putLong(InternetArchiveFragment.ARG_SPACE, spaceId)
            }
        }

        @JvmStatic
        fun newInstance() = newInstance(InternetArchiveFragment.ARG_VAL_NEW_SPACE)
    }
}