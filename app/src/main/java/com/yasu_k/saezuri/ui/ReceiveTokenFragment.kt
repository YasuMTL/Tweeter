package com.yasu_k.saezuri.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.yasu_k.saezuri.data.source.ReceiveTokenRepository
import com.yasu_k.saezuri.data.source.TweetRepository
import com.yasu_k.saezuri.databinding.FragmentReceiveTokenBinding

class ReceiveTokenFragment : Fragment() {
    private var _binding: FragmentReceiveTokenBinding? = null
    private val binding get() = _binding!!

    private val tweetRepository = TweetRepository()
    private val receiveTokenRepository = ReceiveTokenRepository()

    private val sharedViewModel: TweetViewModel by activityViewModels() {
        TweetViewModelFactory(tweetRepository, receiveTokenRepository)
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
            btnLogin.setOnClickListener {
                sharedViewModel.login(requireContext(), lifecycleScope)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}