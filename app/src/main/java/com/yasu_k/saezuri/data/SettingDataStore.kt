package com.yasu_k.saezuri.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private const val TWITTER_PREFERENCES_NAME = "twitter_preferences"
private val Context.dataStore : DataStore<Preferences> by preferencesDataStore(
    name = TWITTER_PREFERENCES_NAME
)

class SettingDataStore(context: Context) {
    private val IS_LOGGED_IN_MANAGER = booleanPreferencesKey("is_logged_in_manager")
    //How to store configuration builder?
    private val TWITTER_TOKEN_MANAGER = stringPreferencesKey("twitter_token_manager")
    private val TWITTER_TOKEN_SECRET_MANAGER = stringPreferencesKey("twitter_token_secret_manager")

    //Read Boolean preference
    val preferenceIsLoggedInFlow: Flow<Boolean> = context.dataStore.data
        .catch {
            if(it is IOException) {
                it.printStackTrace()
                emit(emptyPreferences())
            } else {
                throw it
            }
        }
        .map { preferences ->
            preferences[IS_LOGGED_IN_MANAGER] ?: false
        }

    val preferenceTokenFlow: Flow<String> = context.dataStore.data
        .catch {
            if(it is IOException) {
                it.printStackTrace()
                emit(emptyPreferences())
            } else {
                throw it
            }
        }
        .map { preferences ->
            preferences[TWITTER_TOKEN_MANAGER] ?: ""
        }

    val preferenceTokenSecretFlow: Flow<String> = context.dataStore.data
        .catch {
            if(it is IOException) {
                it.printStackTrace()
                emit(emptyPreferences())
            } else {
                throw it
            }
        }
        .map { preferences ->
            preferences[TWITTER_TOKEN_SECRET_MANAGER] ?: ""
        }

    suspend fun saveLoginInfoToPreferencesStore(isLoggedInManager: Boolean, context: Context) {
        context.dataStore.edit { preferences ->
            preferences[IS_LOGGED_IN_MANAGER] = isLoggedInManager
        }
    }

    suspend fun saveTokenToPreferencesStore(
        twitterToken: String,
        context: Context) {
        context.dataStore.edit { preferences ->
            preferences[TWITTER_TOKEN_MANAGER] = twitterToken
        }
    }

    suspend fun saveTokenSecretToPreferencesStore(
        twitterTokenSecret: String,
        context: Context) {
        context.dataStore.edit { preferences ->
            preferences[TWITTER_TOKEN_SECRET_MANAGER] = twitterTokenSecret
        }
    }

    suspend fun removeLoginStateFromPreferencesStore(context: Context) {
        context.dataStore.edit { preferences ->
            preferences.remove(IS_LOGGED_IN_MANAGER)
        }
    }

    suspend fun removeTokenFromPreferencesStore(context: Context) {
        context.dataStore.edit { preferences ->
            preferences.remove(TWITTER_TOKEN_MANAGER)
        }
    }

    suspend fun removeTokenSecretFromPreferencesStore(context: Context) {
        context.dataStore.edit { preferences ->
            preferences.remove(TWITTER_TOKEN_SECRET_MANAGER)
        }
    }
}