package com.pocket.app.reader.internal.initial

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.ideashower.readitlater.databinding.FragmentReaderInitialBinding
import com.pocket.app.reader.Reader
import com.pocket.sdk.util.AbsPocketFragment
import com.pocket.util.android.navigateSafely
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class InitialFragment : AbsPocketFragment(), Reader.NavigationEventHandler {

    private val navController: NavController?
        get() = (parentFragment as? NavHostFragment)?.navController

    private val viewModel: InitialViewModel by viewModels()

    private var _binding: FragmentReaderInitialBinding? = null
    private val binding: FragmentReaderInitialBinding
        get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showsDialog = false
    }

    override fun onCreateViewImpl(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentReaderInitialBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("Navigation", "InitialFragment")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun handleNavigationEvent(event: Reader.NavigationEvent) {
        when (event) {
            is Reader.NavigationEvent.GoToArticle -> {
                navController?.navigateSafely(InitialFragmentDirections.switchToArticle(event.url))
            }
            is Reader.NavigationEvent.GoToCollection -> {
                navController?.navigateSafely(InitialFragmentDirections.switchToCollection(event.url))
            }
            is Reader.NavigationEvent.GoToOriginalWeb -> {
                navController?.navigateSafely(InitialFragmentDirections.switchToOriginalWeb(event.url))
            }
        }
    }
}