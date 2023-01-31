package com.yasunari_k.saezuri.ui

import android.content.Context
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
import com.yasunari_k.saezuri.MyApplication
import com.yasunari_k.saezuri.data.SettingDataStore
import com.yasunari_k.saezuri.data.source.ReceiveTokenRepository
import com.yasunari_k.saezuri.data.source.TweetRepository
import com.yasunari_k.saezuri.databinding.FragmentReceiveTokenBinding
import kotlinx.coroutines.launch
import javax.inject.Inject

class ReceiveTokenFragment : Fragment() {
    private var _binding: FragmentReceiveTokenBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var tweetRepository: TweetRepository
    @Inject
    lateinit var receiveTokenRepository: ReceiveTokenRepository

    private val settingDataStore: SettingDataStore by lazy {
        SettingDataStore(requireContext())
    }

    private val sharedViewModel: TweetViewModel by activityViewModels {
        TweetViewModelFactory(tweetRepository, receiveTokenRepository, settingDataStore)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        println("ReceiveToken: onCreate called")
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        (requireActivity().application as MyApplication).appComponent.inject(this)
    }

    override fun onStop() {
        super.onStop()
        println("ReceiveToken: onStop called")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        println("ReceiveToken: onCreateView called")
        val isLoggedIn = sharedViewModel.uiState.value.isLoggedIn
        println("isLoggedIn = $isLoggedIn")
        _binding = FragmentReceiveTokenBinding.inflate(inflater, container, false)
        val view = binding.root

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        println("ReceiveToken: onViewCreated called")
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
                    if (root.findNavController().currentDestination?.id == root.findNavController().graph.startDestinationId) {
                        println("Fragment: 1 ReceiveToken -> Tweet")
                        root.findNavController().navigate(action)
                    }
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
                println("Fragment: 2 ReceiveToken -> Tweet")
                root.findNavController().navigate(action)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        println("ReceiveToken: onDestroyView is called")
    }

    override fun onDestroy() {
        super.onDestroy()
        println("ReceiveToken: onDestroy is called")
    }
}