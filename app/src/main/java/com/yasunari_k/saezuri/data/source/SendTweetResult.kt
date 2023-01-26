package com.yasunari_k.saezuri.data.source

import twitter4j.TwitterException

data class SendTweetResult(
    var statusCode: Int,
    var twitterException: TwitterException? = null
)