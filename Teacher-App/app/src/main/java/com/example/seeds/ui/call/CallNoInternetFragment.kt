package com.example.seeds.ui.noInternet

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.seeds.R
import com.example.seeds.databinding.FragmentCallNoInternetBinding
import com.example.seeds.databinding.FragmentNoInternetBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CallNoInternetFragment : Fragment() {
    private lateinit var binding: FragmentCallNoInternetBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentCallNoInternetBinding.inflate(layoutInflater)
        binding.lifecycleOwner = viewLifecycleOwner

        binding.retryButton.setOnClickListener {
            if (isAdded) {
                requireActivity().supportFragmentManager.popBackStack()
            }
        }
        return binding.root
    }
}