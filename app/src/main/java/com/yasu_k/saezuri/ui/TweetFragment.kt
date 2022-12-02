package com.yasu_k.saezuri.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import com.google.android.gms.ads.AdView
import com.yasu_k.saezuri.*
import com.yasu_k.saezuri.data.SettingDataStore
import com.yasu_k.saezuri.data.source.ReceiveTokenRepository
import com.yasu_k.saezuri.data.source.TweetRepository
import com.yasu_k.saezuri.data.source.TweetRepository.Companion.imagesPathList
import com.yasu_k.saezuri.databinding.FragmentTweetBinding
import kotlinx.coroutines.launch
import twitter4j.auth.OAuthAuthorization
import twitter4j.conf.ConfigurationContext

private var mAdView: AdView? = null

class TweetFragment : Fragment(), View.OnClickListener {

    private var _binding: FragmentTweetBinding? = null
    private val binding get() = _binding!!

    private val tweetRepository = TweetRepository()
    private val receiveTokenRepository = ReceiveTokenRepository()

    private val sharedViewModel: TweetViewModel by activityViewModels {
        TweetViewModelFactory(tweetRepository, receiveTokenRepository, SettingDataStore)
    }

    private val SettingDataStore: SettingDataStore by lazy {
        SettingDataStore(requireContext())
    }

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

            btnLogOut.setOnClickListener {
                sharedViewModel.clearTokenInfoFromPreferenceStore(requireContext())
                val action =
                    TweetFragmentDirections.actionTweetFragmentToReceiveTokenFragment()
                root.findNavController().navigate(action)
            }

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

    private fun setupButtons() {
        binding.apply {
            btnSendTweet.setOnClickListener(this@TweetFragment)
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
                lifecycleScope.launch {
                    sharedViewModel.login(requireContext(), lifecycleScope)
                }
            }

            R.id.btnSendTweet -> sharedViewModel.sendTweet(binding.etTweet.text.toString())

            R.id.btnLogOut -> {
                sharedViewModel.logout()
                lifecycleScope.launch {
                    SettingDataStore.saveLoginInfoToPreferencesStore(isLoggedInManager = false, requireContext())
                }
                val action = TweetFragmentDirections.actionTweetFragmentToReceiveTokenFragment()
                binding.root.findNavController().navigate(action)
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

    private fun clearOutEtTweet() {
        binding.etTweet.setText("")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}