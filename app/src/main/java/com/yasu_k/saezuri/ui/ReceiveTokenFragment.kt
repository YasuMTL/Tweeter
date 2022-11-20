package com.yasu_k.saezuri.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.yasu_k.saezuri.databinding.FragmentReceiveTokenBinding

class ReceiveTokenFragment : Fragment() {
    private var _binding: FragmentReceiveTokenBinding? = null
    private val binding get() = _binding!!
    private val sharedViewModel: TweetViewModel by activityViewModels()//Scoped to the activity rather than the current fragment

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
            btnBackToTweet.setOnClickListener {
                //TODO do something
            }
        }
    }
}