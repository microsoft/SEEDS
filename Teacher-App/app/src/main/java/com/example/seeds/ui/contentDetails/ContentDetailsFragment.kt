package com.example.seeds.ui.contentDetails

import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import com.example.seeds.R
import com.example.seeds.databinding.FragmentContactsBinding
import com.example.seeds.databinding.FragmentContentDetailsBinding
import com.example.seeds.ui.BaseFragment
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ContentDetailsFragment : BaseFragment() {
    private lateinit var binding: FragmentContentDetailsBinding
    private val args: ContentDetailsFragmentArgs by navArgs()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentContentDetailsBinding.inflate(inflater)
        binding.content = args.content
        binding.contentAudio.player = ExoPlayer.Builder(requireContext()).build()
        val videoUri = Uri.parse("https://seedsblob.blob.core.windows.net/output-original/${args.content.id}.mp3")
        val mediaItem: MediaItem = MediaItem.fromUri(videoUri)
        binding.contentAudio.player?.setMediaItem(mediaItem)
        binding.contentAudio.player?.prepare()
        return binding.root
    }

    override fun onDestroyView() {
        logMessage("Music player released ${args.content.id} ${args.content.title} ${binding.contentAudio.player?.contentPosition} ${binding.contentAudio.player?.contentDuration}")
        binding.contentAudio.player?.stop()
        binding.contentAudio.player?.release()
        super.onDestroyView()
    }

    override fun onStart() {
        logMessage("onStart")
        super.onStart()
    }

    override fun onStop() {
        logMessage("onStop")
        super.onStop()
    }
}