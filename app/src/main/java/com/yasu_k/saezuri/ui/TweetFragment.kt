package com.yasu_k.saezuri.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.ext.SdkExtensions.getExtensionVersion
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.CallSuper
import androidx.annotation.RequiresApi
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

    private var wasVideoChosen = false
    private var werePhotosChosen = false
    private var _binding: FragmentTweetBinding? = null
    private val binding get() = _binding!!

    private val tweetRepository = TweetRepository()
    private val receiveTokenRepository = ReceiveTokenRepository()

    private val sharedViewModel: TweetViewModel by activityViewModels {
        TweetViewModelFactory(tweetRepository, receiveTokenRepository, settingDataStore)
    }

    private val settingDataStore: SettingDataStore by lazy {
        SettingDataStore(requireContext())
    }

    private val statusCode: MutableLiveData<Int> by lazy {
        MutableLiveData()
    }

    //private var chosenUri: Uri? = null
    private var chosenURIs = mutableListOf<Uri>()
    private val getVideoContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        println("uri = $uri")
        //chosenUri = uri
        if (uri != null) {
            chosenURIs.add(uri)
        }
    }

    private val getPhotosContent = registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(4)) { uriList ->
//    private val getPhotosContent = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uriList ->
        println("uri = $uriList uriList.size() = ${uriList.size}")
        //chosenUri = uriList.first()
        uriList.forEach {
            chosenURIs.add(it)
        }

        //TODO: What to do when there are more than one image?

        val message = if (uriList.isEmpty()) {
            "No media selected"
        } else {
            "Number of items selected: ${uriList.size}"
        }

        println(message)
        showLongToast(message)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun isPhotoPickerAvailable(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            true
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getExtensionVersion(Build.VERSION_CODES.R) >= 2
        } else {
            false
        }
    }

    //val takeImageResult = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
    private val takeImageResult = registerForActivityResult(TakePictureWithUriContract()) { (success, uri) ->
        Log.i(javaClass.name, "success = $success, uri = $uri")
        if (!success) {
            val indexOfLastElement = imagesPathList.size - 1
            imagesPathList.removeAt(indexOfLastElement)
            val message = "Last element (URI) of the imagesPathList was eliminated"
            println(message)
            showLongToast(message)
        }
    }

    private val takeVideoResult = registerForActivityResult(ActivityResultContracts.CaptureVideo()) { success ->
        Log.i(javaClass.name, "success = $success")
        if (!success) {
            showLongToast("Video was not captured for some reason")
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

    private lateinit var tempUri: Uri

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        println("Tweet: onCreate called")

        val callback = requireActivity().onBackPressedDispatcher.addCallback(this) {
            // Handle the back button event
            //Toast.makeText(requireContext(), "Back button was pressed!", Toast.LENGTH_SHORT).show()
            println("Back button was pressed!")
        }

        //callback.remove()
    }
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

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
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

        LoginInfo.mOauth = OAuthAuthorization(ConfigurationContext.getInstance())

        statusCode.observe(viewLifecycleOwner) { code ->
            when (code) {
                200 -> {
                    showLongToast(getString(R.string.tweet_sent_success))
                    removeUploadedFiles()
                    clearOutEtTweet()
                }

                400 -> {
                    showLongToast(getString(R.string.request_invalid))
                }

                403 -> {
                    val twitterException = sharedViewModel.getStoredTwitterException()
                    val errorMessage = getMessageThroughErrorCode(twitterException?.errorCode ?: -1)
                    println("errorMessage = $errorMessage")
                    showLongToast(errorMessage)
                }

                503 -> {
                    showLongToast(getString(R.string.twitter_unavailable))
                }
                else -> {
                    showLongToast("Something wrong happens...")
                    removeUploadedFiles()
                    hideProgressBar()
                }
            }
            enableButtons()
            hideWarningWhileUploading()
            hideProgressBar()
        }

        val isPhotoPickerAvailable = isPhotoPickerAvailable()
        println("isPhotoPickerAvailable() = $isPhotoPickerAvailable")
    }

    private fun getMessageThroughErrorCode(errorCode: Int): String {
        val errorMessage: String = when (errorCode)
        {
            170 -> {
                showLongToast(getString(R.string.no_text_to_tweet))
                getString(R.string.no_text_to_tweet)
            }

            193 -> {
                showLongToast(getString(R.string.media_is_too_large))
                getString(R.string.media_is_too_large)
            }

            -1 -> {
                showLongToast(getString(R.string.unknown_error))
                getString(R.string.unknown_error)
            }

            else -> {
                getString(R.string.unknown_error)
            }
        }

        return errorMessage
    }

    private fun removeUploadedFiles() {
        chosenURIs.clear()
        showLongToast("Uploaded files were removed")
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
        println("Tweet: onPause is called")
        if (mAdView != null) {
            mAdView!!.pause()
        }
        super.onPause()
    }

    /** Called when returning to the activity  */
    override fun onResume() {
        super.onResume()
        println("Tweet: onResume is called")
        if (mAdView != null) {
            mAdView!!.resume()
        }
    }

    /** Called before the activity is destroyed  */
    override fun onDestroy() {
        println("Tweet: onDestroy is called")
        if (mAdView != null) {
            mAdView!!.destroy()
        }
        super.onDestroy()
    }

    override fun onClick(view: View) {
        println("TweetFragment: onClick() is fired!")
        when (view.id) {
//            R.id.btnLogin -> {
//                //checkIfILoggedIn()
//                lifecycleScope.launch {
//                    sharedViewModel.login(requireContext(), lifecycleScope)
//                }
//            }

            R.id.btnSendTweet -> {
                binding.indeterminateBar.visibility = View.VISIBLE

                lifecycleScope.launch {
                    if (chosenURIs.isEmpty()) {
                        statusCode.value =
                            sharedViewModel.sendTweet(
                                binding.etTweet.text.toString(),
                                requireContext().contentResolver
                            )
                    } else {
                        showLongToast("Uploading...")
                        disableButtons()
                        showWarningWhileUploading()
                        statusCode.value =
                            sharedViewModel.sendTweetWithChosenUri(
                                binding.etTweet.text.toString(),
                                requireContext().contentResolver,
                                chosenURIs
                            )
                        //chosenURIs.clear()
                    }
                    clearOutMediaFiles()
                }
            }

            R.id.btnLogOut -> {
                println("LogOut button was pressed!")
                sharedViewModel.logout()

                lifecycleScope.launch {
                    settingDataStore.saveLoginInfoToPreferencesStore(isLoggedInManager = false, requireContext())
                    //sharedViewModel.resetUiState()
                }
                val action = TweetFragmentDirections.actionTweetFragmentToReceiveTokenFragment()
                println("Fragment: Tweet -> ReceiveToken")
                binding.root.findNavController().navigate(action)
            }

            R.id.btnClear -> {
                clearOutEtTweet()
                sharedViewModel.clearUploadedMediaFiles()
                println("Uploaded files (URIs) were removed from memory")
            }

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

    private fun showWarningWhileUploading() {
        binding.warningMessage.visibility = View.VISIBLE
        println("warningMessage is visible")
    }

    private fun hideWarningWhileUploading() {
        binding.warningMessage.visibility = View.GONE
        println("warningMessage is gone")
    }

    private fun disableButtons() {
        binding.apply {
            btnSendTweet.isEnabled = false
            btnClear.isEnabled = false
            btnLogOut.isEnabled = false
            btnUploadPhotoVideo.isEnabled = false
        }
    }

    private fun enableButtons() {
        binding.apply {
            btnSendTweet.isEnabled = true
            btnClear.isEnabled = true
            btnLogOut.isEnabled = true
            btnUploadPhotoVideo.isEnabled = true
        }
    }

    private fun clearOutEtTweet() {
        binding.etTweet.setText("")
        hideProgressBar()
        clearOutMediaFiles()
    }

    private fun clearOutMediaFiles() {
        werePhotosChosen = false
        wasVideoChosen = false
    }

    private fun hideProgressBar() {
        binding.indeterminateBar.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        println("Tweet: onDestroyView is called")
    }

    override fun onOptionClick(whichOption: Option) {
        when (whichOption) {
            Option.SELECT_IMAGES -> {
                if (wasVideoChosen) {
                    showLongToast("One video is already picked up! You cannot upload video and photo together!")
                } else {
                    showLongToast("Choose photos!")
                    werePhotosChosen = true
                    getPhotosContent.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
            }

            Option.SELECT_ONE_VIDEO -> {
                if (werePhotosChosen) {
                    showLongToast("Images were already chosen! You cannot upload images and video together!")
                } else {
                    showLongToast("Pick up a video!")
                    if (wasVideoChosen) {
                        showLongToast("One video has already been chosen. This action will replace the video with new one.")
                    }
                    wasVideoChosen = true
                    getVideoContent.launch("video/*")
                }
            }

            Option.TAKE_ONE_PHOTO -> {
                if (wasVideoChosen) {
                    showLongToast("One video is already picked up! You cannot upload video and photo together!")
                } else {
                    showLongToast("Take one photo!")
                    werePhotosChosen = true
                    sharedViewModel.takeOnePhoto(requireContext(), takeImageResult)
                }
            }

            Option.CAPTURE_ONE_VIDEO -> {
                if (werePhotosChosen) {
                    showLongToast("Images were already chosen! You cannot upload images and video together!")
                } else {
                    showLongToast("Capture a video!")
                    if (wasVideoChosen) {
                        showLongToast("One video has already been chosen. This action will replace the video with new one.")
                    }
                    wasVideoChosen = true
                    sharedViewModel.takeOneVideo(requireContext(), takeVideoResult)
                }
            }

            Option.NOT_CHOSEN -> {
                showLongToast("Nothing is chosen")
            }
        }
    }

    private fun showLongToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }
}