package com.yasunari_k.saezuri

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import com.yasunari_k.saezuri.R

class Tweet : AppCompatActivity() {
//    @Inject
//    lateinit var spTwitterToken: SharedPreferences
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        //(application as MyApplication).appComponent.inject(this)//Dagger2

        super.onCreate(savedInstanceState)
        setContentView(R.layout.tweet)

        // Get the navigation host fragment from this Activity
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment

        // Instantiate the navController using the NavHostFragment
        navController = navHostFragment.navController

        // Put also the tweetFragment at the top level of nav graph so that the up button be hidden
        appBarConfiguration = AppBarConfiguration(setOf(
            R.id.receiveTokenFragment,
            R.id.tweetFragment
        ))

        // Make sure actions in the ActionBar get propagated to the NavController
        setupActionBarWithNavController(navController, appBarConfiguration)
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}