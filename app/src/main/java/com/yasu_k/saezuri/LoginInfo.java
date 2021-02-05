package com.yasu_k.saezuri;

import twitter4j.auth.OAuthAuthorization;
import twitter4j.auth.RequestToken;

public final class LoginInfo {
    private LoginInfo(){}

    // Login
    public static OAuthAuthorization mOauth;
    public static RequestToken mRequest;

    // Twitter
    public static final String oAuthConsumerKey = "LJi96Jk8iC8HsipGdi7TazT9q";
    public static final String oAuthConsumerSecret = "N9Rf7eTxmy4YLHtaoQeAVoImowcAbFc0KYsUEoUTipi8Q80y6L";
}
