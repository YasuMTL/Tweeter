package com.yasu_k.saezuri;

import twitter4j.auth.OAuthAuthorization;
import twitter4j.auth.RequestToken;

public final class LoginInfo {
    private LoginInfo(){}

    // Login
    public static OAuthAuthorization mOauth;
    public static RequestToken mRequest;
}
