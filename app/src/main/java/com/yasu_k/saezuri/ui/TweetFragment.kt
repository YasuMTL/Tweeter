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

private var _binding: FragmentTweetBinding? = null
private val binding get() = _binding!!

private var mAdView: AdView? = null

class TweetFragment : Fragment(), View.OnClickListener {

    private val viewModel: TweetViewModel by activityViewModels()//Scoped to the activity rather than the current fragment

//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setHasOptionsMenu(true)
//    }

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

        //TODO: Create a global variable to know if the user is logged in or not
        //showAndHideLayouts(isUserAlreadyLoggedIn)

        //hyperlink
        binding.tvPrivacyPolicy.movementMethod = LinkMovementMethod.getInstance()

        imagesPathList = ArrayList()
        LoginInfo.mOauth = OAuthAuthorization(ConfigurationContext.getInstance())

        binding.etTweet.addTextChangedListener(object : TextWatcher {
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
                viewModel.login()
            }
            R.id.btnSendTweet -> viewModel.sendTweet()
            R.id.btnLogOut -> {
                viewModel.logout()
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == AppCompatActivity.RESULT_OK) {
            try {
                if (requestCode == Code.RESULT_LOAD_IMAGE) {
                    if (null != data) {
                        var imagePath: String?
                        if (data.data != null) {
                            //When an image is picked
                            val mImageUri = data.data
                            imagePath = getPathFromUri(requireContext(), mImageUri)
                            sizePhotoCheck(imagePath)
                        } else {
                            //When multiple images are picked
                            if (data.clipData != null) {
                                println("++data: " + data.clipData!!.itemCount) // Get count of image here.
                                for (i in 0 until data.clipData!!.itemCount) {
                                    val selectedImage = data.clipData!!.getItemAt(i).uri
                                    //String type list which contains file path of each selected image file
                                    imagePath = getPathFromUri(requireContext(), selectedImage)
                                    sizePhotoCheck(imagePath)
                                    println("selectedImage: $selectedImage")
                                }
                            }
                        }
                        println("data.getData(): " + data.data)
                    } else {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.not_picked_images),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else if (requestCode == Code.RESULT_LOAD_VIDEO) {
                    val selectedImageUri = data!!.data

                    // MEDIA GALLERY
                    selectedVideoPath = getPathFromUri(requireContext(), selectedImageUri)
                    sizeVideoCheck(selectedVideoPath)
                } else if (requestCode == Code.REQUEST_VIDEO_CAPTURE) {
                    val newVideoUri = data!!.data

                    // MEDIA GALLERY
                    selectedVideoPath = getPathFromUri(requireContext(), newVideoUri)
                    sizeVideoCheck(selectedVideoPath)
                } else if (requestCode == Code.REQUEST_TAKE_PHOTO) {
                    if (MediaOptions.cameraFile != null) {
                        //registerDatabase(cameraFile);
                        println("cameraFile: " + MediaOptions.cameraFile)
                        println("cameraFile.getAbsolutePath(): " + MediaOptions.cameraFile!!.absolutePath)
                        println("mCapturedImageURI: " + MediaOptions.mCapturedImageURI)
                        println("mCapturedImageURI.getAuthority(): " + (MediaOptions.mCapturedImageURI?.authority))
                        val imagePath = MediaOptions.cameraFile!!.absolutePath
                        if (MediaOptions.cameraFile!!.length() <= 5000000) {
                            imagesPathList!!.add(imagePath)
                        } else {
                            imagesPathList!!.clear()
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.size_too_large),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } else {
                        Toast.makeText(requireContext(), getString(R.string.fail_photo), Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), getString(R.string.attach_fail), Toast.LENGTH_SHORT).show()
            }
        }
    } //END onActivityResult()



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