package com.yasu_k.saezuri.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.CallSuper
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import com.google.android.gms.ads.AdView
import com.yasu_k.saezuri.LoginInfo
import com.yasu_k.saezuri.R
import com.yasu_k.saezuri.SetAdView
import com.yasu_k.saezuri.TextCounter
import com.yasu_k.saezuri.data.SettingDataStore
import com.yasu_k.saezuri.data.source.ImageFileHelper
import com.yasu_k.saezuri.data.source.ReceiveTokenRepository
import com.yasu_k.saezuri.data.source.TweetRepository
import com.yasu_k.saezuri.data.source.TweetRepository.Companion.imagesPathList
import com.yasu_k.saezuri.databinding.FragmentTweetBinding
import kotlinx.coroutines.launch
import twitter4j.auth.OAuthAuthorization
import twitter4j.conf.ConfigurationContext

private var mAdView: AdView? = null

class TweetFragment : Fragment(),
    View.OnClickListener,
    MediaDialogFragment.NoticeDialogListener {

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

    private val statusCode: MutableLiveData<Int> by lazy {
        MutableLiveData()
    }

    //    val takeImageResult: ActivityResultLauncher<Uri> = registerForActivityResult(
//        ActivityResultContracts.TakePicture()) { success ->
//        Log.i(javaClass.name, "success = $success")
//        sizePhotoCheck(tempUri?.path)
//    }
    //val takeImageResult = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
    private val takeImageResult = registerForActivityResult(TakePictureWithUriContract()) { (success, uri) ->
        Log.i(javaClass.name, "success = $success, uri = $uri")
        if (!success) {
            val indexOfLastElement = imagesPathList.size - 1
            imagesPathList.removeAt(indexOfLastElement)
            val message = "Last element (URI) of the imagesPathList was eliminated"
            println(message)
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }

    private val takeVideoResult = registerForActivityResult(ActivityResultContracts.CaptureVideo()) { success ->
    //private val takeVideoResult = registerForActivityResult(CaptureVideoWithUriContract()) { success ->
        Log.i(javaClass.name, "success = $success" /*url = $uri"*/)
        if (!success) {
            Toast.makeText(requireContext(), "Video was not captured for some reason", Toast.LENGTH_SHORT).show()
//            val indexOfLastElement = imagesPathList.size - 1
//            imagesPathList.removeAt(indexOfLastElement)
//            val message = "Last element (URI) of the imagesPathList was eliminated"
//            println(message)
//            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }

    //test
    class TakePictureWithUriContract : ActivityResultContract<Uri, Pair<Boolean, Uri>>() {

        private lateinit var imageUri: Uri

        @CallSuper
        override fun createIntent(context: Context, input: Uri): Intent {
            imageUri = input
            return Intent(MediaStore.ACTION_IMAGE_CAPTURE).putExtra(MediaStore.EXTRA_OUTPUT, input)
        }

        override fun getSynchronousResult(
            context: Context,
            input: Uri
        ): SynchronousResult<Pair<Boolean, Uri>>? = null

        @Suppress("AutoBoxing")
        override fun parseResult(resultCode: Int, intent: Intent?): Pair<Boolean, Uri> {
            return (resultCode == Activity.RESULT_OK) to imageUri
        }
    }

    class CaptureVideoWithUriContract : ActivityResultContract<Uri, Pair<Boolean, Uri>>() {

        private lateinit var videoUri: Uri

        @CallSuper
        override fun createIntent(context: Context, input: Uri): Intent {
            videoUri = input
            return Intent(MediaStore.ACTION_VIDEO_CAPTURE).putExtra(MediaStore.EXTRA_OUTPUT, input)
        }

        override fun getSynchronousResult(
            context: Context,
            input: Uri
        ): SynchronousResult<Pair<Boolean, Uri>>? = null

        @Suppress("AutoBoxing")
        override fun parseResult(resultCode: Int, intent: Intent?): Pair<Boolean, Uri> {
            return (resultCode == Activity.RESULT_OK) to videoUri
        }
    }


    lateinit var tempUri: Uri

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTweetBinding.inflate(inflater, container, false)
        val view = binding.root

        tempUri = ImageFileHelper.getImageUri(requireContext())

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

        //imagesPathList = ArrayList()
        LoginInfo.mOauth = OAuthAuthorization(ConfigurationContext.getInstance())

        statusCode.observe(viewLifecycleOwner) { code ->
            when (code) {
                200 -> {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.tweet_sent_success),
                        Toast.LENGTH_SHORT
                    ).show()
                    clearOutEtTweet()
                }

                400 -> {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.request_invalid),
                        Toast.LENGTH_SHORT
                    ).show()
                }

                403 -> {

                }

                503 -> {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.twitter_unavailable),
                        Toast.LENGTH_LONG
                    ).show()
                }
                else -> Toast.makeText(requireContext(), "Something wrong happens...", Toast.LENGTH_SHORT).show()
            }
        }

        /*
        else if (statusCode == 403)
        {
            when (errorCode)
            {
                //170 ->
                    /*Toast.makeText(
                    applicationContext,
                    getString(R.string.no_text_to_tweet),
                    Toast.LENGTH_SHORT
                ).show()*/

                //193 ->
                 /*Toast.makeText(
                    applicationContext,
                    getString(R.string.media_is_too_large),
                    Toast.LENGTH_LONG
                ).show()*/

                //-1 ->
                /*Toast.makeText(
                    applicationContext,
                    getString(R.string.unknown_error),
                    Toast.LENGTH_LONG
                ).show()*/

                else -> {}
            }
        }
        * */
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

            R.id.btnSendTweet -> {
                lifecycleScope.launch {
                    statusCode.value = sharedViewModel.sendTweet(binding.etTweet.text.toString(), requireContext().contentResolver)
                }
            }

            R.id.btnLogOut -> {
                sharedViewModel.logout()
                lifecycleScope.launch {
                    SettingDataStore.saveLoginInfoToPreferencesStore(isLoggedInManager = false, requireContext())
                }
                val action = TweetFragmentDirections.actionTweetFragmentToReceiveTokenFragment()
                binding.root.findNavController().navigate(action)
            }

            R.id.btnClear -> clearOutEtTweet()

            //R.id.btnUploadPhotoVideo -> { sharedViewModel.uploadPhotoVideo(requireContext()) }
            R.id.btnUploadPhotoVideo -> {
                //sharedViewModel.uploadPhotoVideo(showMediaOptions)
                if(isAdded) {
                    val dialog = MediaDialogFragment()
                    dialog.show(childFragmentManager, "MediaOptionsDialog")
                } else {
                    println("${javaClass.name}: This fragment is not added yet to the activity")
                }
                //TODO test
                //takeImageResult.launch(tempUri)
            }
        }
    }

    // Runtime Permission check
//    private fun requestTwoPermissions() {
//        // If at least one of two permissions isn't yet granted
//        if (ActivityCompat.checkSelfPermission(
//                requireContext(),
//                Manifest.permission.READ_EXTERNAL_STORAGE
//            ) != PackageManager.PERMISSION_GRANTED ||
//            ActivityCompat.checkSelfPermission(
//                requireContext(),
//                Manifest.permission.CAMERA
//            ) != PackageManager.PERMISSION_GRANTED
//        ) {
//            ActivityCompat.requestPermissions(
//                requireActivity(), arrayOf(
//                    Manifest.permission.READ_EXTERNAL_STORAGE,
//                    Manifest.permission.CAMERA
//                ), Code.PERMISSION_REQUEST_CODE
//            )
//        }
//    }

    private fun clearOutEtTweet() {
        binding.etTweet.setText("")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onOptionClick(whichOption: Option) {
        when (whichOption) {
            Option.SELECT_IMAGES -> {
                Toast.makeText(requireContext(), "Choose photos!", Toast.LENGTH_SHORT).show()

            }
            Option.SELECT_ONE_VIDEO -> {
                Toast.makeText(requireContext(), "Pick up a video!", Toast.LENGTH_SHORT).show()
            }
            Option.TAKE_ONE_PHOTO -> {
                //TODO: Call a function to take a photo from the view model instance
                Toast.makeText(requireContext(), "Take one photo!", Toast.LENGTH_SHORT).show()
                sharedViewModel.takeOnePhoto(requireContext(), takeImageResult)
            }
            Option.CAPTURE_ONE_VIDEO -> {
                Toast.makeText(requireContext(), "Capture a video!", Toast.LENGTH_SHORT).show()
                sharedViewModel.takeOneVideo(requireContext(), takeVideoResult)
            }
            Option.NOT_CHOSEN -> {
                Toast.makeText(requireContext(), "Unknown!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}