package com.yasu_k.saezuri.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.gms.ads.AdView
import com.yasu_k.saezuri.*
import com.yasu_k.saezuri.data.source.TweetRepository.Companion.imagesPathList
import com.yasu_k.saezuri.data.source.TweetRepository.Companion.selectedVideoPath
import com.yasu_k.saezuri.databinding.FragmentTweetBinding
import twitter4j.auth.OAuthAuthorization
import twitter4j.conf.ConfigurationContext

private var mAdView: AdView? = null

class TweetFragment : Fragment(), View.OnClickListener {

    private var _binding: FragmentTweetBinding? = null
    private val binding get() = _binding!!
    private val sharedViewModel: TweetViewModel by activityViewModels()//Scoped to the activity rather than the current fragment

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTweetBinding.inflate(inflater, container, false)
        val view = binding.root

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adView = SetAdView(binding.adView, requireContext())
        adView.initAd()
        setupButtons()

        binding.apply {
            viewModel = sharedViewModel
            //hyperlink
            tvPrivacyPolicy.movementMethod = LinkMovementMethod.getInstance()



            etTweet.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(
                    enteredText: CharSequence,
                    start: Int,
                    before: Int,
                    count: Int
                ) {
                    val textColor: Int
                    val length = 280 - TextCounter().getTextLength(enteredText)
                    if (length < 0) {
                        textColor = Color.RED
                        binding.btnSendTweet.isEnabled = false
                        binding.btnSendTweet.setTextColor(Color.GRAY)
                    } else {
                        textColor = Color.GRAY
                        binding.btnSendTweet.isEnabled = true
                        binding.btnSendTweet.setTextColor(Color.WHITE)
                    }
                    binding.tvTweetTextCount.setTextColor(textColor)
                    binding.tvTweetTextCount.text = length.toString()
                }

                override fun afterTextChanged(s: Editable) {}
            })
        }

        //TODO: Create a global variable to know if the user is logged in or not
        //showAndHideLayouts(isUserAlreadyLoggedIn)

        imagesPathList = ArrayList()
        LoginInfo.mOauth = OAuthAuthorization(ConfigurationContext.getInstance())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupButtons() {
        binding.apply {
            btnSendTweet.setOnClickListener(this@TweetFragment)
            btnLogin.setOnClickListener(this@TweetFragment)
            btnLogOut.setOnClickListener(this@TweetFragment)
            btnClear.setOnClickListener(this@TweetFragment)
            btnUploadPhotoVideo.setOnClickListener(this@TweetFragment)
        }
    }

    /** Called when leaving the activity  */
    override fun onPause() {
        if (mAdView != null) {
            mAdView!!.pause()
        }
        super.onPause()
    }

    /** Called when returning to the activity  */
    override fun onResume() {
        super.onResume()
        if (mAdView != null) {
            mAdView!!.resume()
        }
    }

    /** Called before the activity is destroyed  */
    override fun onDestroy() {
        if (mAdView != null) {
            mAdView!!.destroy()
        }
        super.onDestroy()
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.btnLogin -> {
                //checkIfILoggedIn()
                sharedViewModel.login()
            }
            R.id.btnSendTweet -> sharedViewModel.sendTweet(binding.etTweet.text.toString(), sharedViewModel.configurationBuilder.value)
            R.id.btnLogOut -> {
                sharedViewModel.logout()
                //TODO Change some layout
//                val redirect = Intent(this@Tweet, Tweet::class.java)
//                startActivity(redirect)
            }
            R.id.btnClear -> clearOutEtTweet()
            R.id.btnUploadPhotoVideo -> {
                requestTwoPermissions()
                val smo = ShowMediaOptions(requireContext())
                smo.showOptionMediaDialog()
            }
        }
    }



    // Runtime Permission check
    private fun requestTwoPermissions() {
        // If at least one of two permissions isn't yet granted
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(), arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA
                ), Code.PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == Code.PERMISSION_REQUEST_CODE) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                //Toast.makeText(this, getString(R.string.permission_granted), Toast.LENGTH_SHORT).show();
                Toast.makeText(requireContext(), getString(R.string.need_permission), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun clearOutEtTweet() {
        binding.etTweet.setText("")
    }

    private fun showAndHideLayouts(isUserLoggedIn: Boolean)
    {
        if (isUserLoggedIn)
        {
            binding.apply {
                btnLogin.visibility = View.INVISIBLE
                btnSendTweet.visibility = View.VISIBLE
                btnLogOut.visibility = View.VISIBLE
                btnClear.visibility = View.VISIBLE
                btnUploadPhotoVideo.visibility = View.VISIBLE
                textInputLayout.visibility = View.VISIBLE
            }
        }
        else
        {
            binding.apply {
                btnLogin.visibility = View.VISIBLE
                btnSendTweet.visibility = View.INVISIBLE
                btnLogOut.visibility = View.INVISIBLE
                btnClear.visibility = View.INVISIBLE
                btnUploadPhotoVideo.visibility = View.INVISIBLE
                textInputLayout.visibility = View.INVISIBLE
            }
        }
    }
}