package com.yasu_k.saezuri.data.source

import android.net.Uri
import androidx.core.net.toUri
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TweetRepositoryTest {

    private lateinit var tweetRepository: TweetRepository
    private lateinit var choosenURIs: MutableList<Uri>
    private lateinit var uri: Uri

    @Before
    fun setUp() {
        tweetRepository = TweetRepository()
        choosenURIs = mutableListOf()
        uri = "".toUri()
    }

    @Test
    fun setUri_UriContainsImage_ImagesPathsListSizeOne() {
        uri = "abc.jpg".toUri()
        choosenURIs.add(uri)
        tweetRepository.setUri(choosenURIs)

        assert(tweetRepository.getImagesPathListSize() == 1)
    }

    @Test
    fun setUri_UriContainsJpg_ImagesPathsListSizeOne() {
        uri = "defg.jpg".toUri()
        choosenURIs.add(uri)
        tweetRepository.setUri(choosenURIs)

        assert(tweetRepository.getImagesPathListSize() == 1)
    }

    @Test
    fun setUri_UriContainsPhoto_ImagesPathsListSizeOne() {
        uri = "hijk.photo".toUri()
        choosenURIs.add(uri)
        tweetRepository.setUri(choosenURIs)

        assert(tweetRepository.getImagesPathListSize() == 1)
//        assert(tweetRepository.getImagesPathListSize() == 0)
    }

    @Test
    fun setUri_UriContainsNoKeyword_ImagesPathsListSizeOne() {
        uri = "abc.pdf".toUri()
        choosenURIs.add(uri)
        tweetRepository.setUri(choosenURIs)

        assert(tweetRepository.getSelectedVideoPathSize() == 7)
    }

    @Test
    fun flushOutUploadedImageVideo_ImagesPathListSize_Zero() {
        uri = "abc.jpg".toUri()
        choosenURIs.add(uri)
        tweetRepository.setUri(choosenURIs)

        tweetRepository.flushOutUploadedImageVideo()

        assert(tweetRepository.getImagesPathListSize() == 0)
    }

    @Test
    fun flushOutUploadedImageVideo_selectedVideoPath_Empty() {
        uri = "abc.defg".toUri()
        choosenURIs.add(uri)
        tweetRepository.setUri(choosenURIs)

        tweetRepository.flushOutUploadedImageVideo()

        assert(tweetRepository.getSelectedVideoPathSize() == 0)
    }
}