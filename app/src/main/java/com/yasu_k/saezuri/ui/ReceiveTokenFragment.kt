package com.yasu_k.saezuri.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import com.yasu_k.saezuri.data.SettingDataStore
import com.yasu_k.saezuri.data.source.ReceiveTokenRepository
import com.yasu_k.saezuri.data.source.TweetRepository
import com.yasu_k.saezuri.databinding.FragmentReceiveTokenBinding
import kotlinx.coroutines.launch

class ReceiveTokenFragment : Fragment() {
    private var _binding: FragmentReceiveTokenBinding? = null
    private val binding get() = _binding!!

    private val tweetRepository = TweetRepository()
    private val receiveTokenRepository = ReceiveTokenRepository()
    private val settingDataStore: SettingDataStore by lazy {
        SettingDataStore(requireContext())
    }

    private val sharedViewModel: TweetViewModel by activityViewModels {
        TweetViewModelFactory(tweetRepository, receiveTokenRepository, settingDataStore)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReceiveTokenBinding.inflate(inflater, container, false)
        val view = binding.root

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            viewModel = sharedViewModel

            sharedViewModel.uiState.asLiveData().observe(viewLifecycleOwner) { loginUiState ->
                if (loginUiState.isLoggedIn && loginUiState.token.isNotBlank()) {
                    sharedViewModel.saveLoginStateToPreferenceStore(true, requireContext())
                    sharedViewModel.saveTokenInfoToPreferenceStore(loginUiState.token, requireContext())
                    sharedViewModel.saveTokenSecretInfoToPreferenceStore(loginUiState.tokenSecret, requireContext())

                    Log.i(javaClass.name, "Store the info to preferences loginUiState.isLoggedIn=${loginUiState.isLoggedIn} loginUiState.token=${loginUiState.token}")
                    val action =
                        ReceiveTokenFragmentDirections.actionReceiveTokenFragmentToTweetFragment()
                    root.findNavController().navigate(action)
                }
            }

            btnLogin.setOnClickListener {
                lifecycleScope.launch {
                    sharedViewModel.login(requireContext(), lifecycleScope)
                }
            }

            val isLoggedIn = sharedViewModel.isLoggedIn()

            if (isLoggedIn) {
                val action =
                    ReceiveTokenFragmentDirections.actionReceiveTokenFragmentToTweetFragment()
                root.findNavController().navigate(action)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}