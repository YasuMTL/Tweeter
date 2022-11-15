package com.yasu_k.saezuri

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.ads.AdView
import com.yasu_k.saezuri.data.source.TweetRepository.Companion.imagesPathList
import com.yasu_k.saezuri.databinding.TweetBinding
import com.yasu_k.saezuri.ui.TweetFragment.Companion.imagesPathList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import twitter4j.StatusUpdate
import twitter4j.TwitterException
import twitter4j.TwitterFactory
import twitter4j.auth.OAuthAuthorization
import twitter4j.conf.ConfigurationBuilder
import twitter4j.conf.ConfigurationContext
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import javax.inject.Inject

//<div>Icons made by <a href="https://www.flaticon.com/<?=_('authors/')?>freepik" title="Freepik">Freepik</a> from <a href="https://www.flaticon.com/" title="Flaticon">www.flaticon.com</a></div>
class Tweet : AppCompatActivity(), View.OnClickListener {
    private var mAdView: AdView? = null
    @Inject
    lateinit var spTwitterToken: SharedPreferences
    private lateinit var binding: TweetBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        (application as MyApplication).appComponent.inject(this)//Dagger2

        super.onCreate(savedInstanceState)

        //(application as MyApplication).appComponent.inject(this)//Dagger2


    }
}