package com.pocket.app.reader.internal.originalweb.overlay.bottomsheet

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import com.ideashower.readitlater.R
import com.ideashower.readitlater.databinding.FragmentOriginalWebBottomSheetBinding
import com.pocket.app.MainActivity
import com.pocket.app.auth.AuthenticationActivity
import com.pocket.app.reader.internal.originalweb.OriginalWebFragment
import com.pocket.app.share.ShareDialogFragment
import com.pocket.app.tags.ItemsTaggingFragment
import com.pocket.sdk.tts.Listen
import com.pocket.sdk.tts.PlayState
import com.pocket.sdk.util.AbsPocketActivity
import com.pocket.sdk.util.AbsPocketBottomSheetDialogFragment
import com.pocket.util.android.stringArg
import com.pocket.util.collectWhenResumed
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class OriginalWebBottomSheetFragment : AbsPocketBottomSheetDialogFragment() {

    @Inject lateinit var listen: Listen

    private val viewModel: OriginalWebBottomSheetViewModel by viewModels()

    private var _binding: FragmentOriginalWebBottomSheetBinding? = null
    private val binding: FragmentOriginalWebBottomSheetBinding
        get() = _binding!!

    private val url by stringArg(ARG_URL)

    private var shouldCloseActivityOnDismiss = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentOriginalWebBottomSheetBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupEventObserver()
        viewModel.onInitialized(url)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        // only finish the activity if the listen player is stopped
        // and we are not in the process of opening it
        if (listen.state().playstate == PlayState.STOPPED && shouldCloseActivityOnDismiss) {
            activity?.finish()
        }
    }

    private fun setupEventObserver() {
        viewModel.events.collectWhenResumed(viewLifecycleOwner) { event ->
            when (event) {
                is OriginalWebBottomSheet.Event.GoBack -> {
                    context?.startActivity(
                        Intent(context, MainActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                    dismiss()
                }
                is OriginalWebBottomSheet.Event.GoToSignIn -> {
                    AuthenticationActivity.startActivity(requireContext(), true)
                }
                is OriginalWebBottomSheet.Event.ShowShare -> {
                    ShareDialogFragment.show(
                        childFragmentManager,
                        url = url,
                        title = event.title,
                    )
                }
                is OriginalWebBottomSheet.Event.SwitchToArticleView -> {
                    OriginalWebFragment.resumeAction = OriginalWebFragment.ResumeAction.SWITCH_TO_ARTICLE_VIEW
                    context?.startActivity(
                        Intent(context, MainActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                    dismiss()
                }
                is OriginalWebBottomSheet.Event.OpenListen -> {
                    listen.trackedControls(null, null).play(event.track)
                    (activity as? AbsPocketActivity)?.expandListenUi()
                    shouldCloseActivityOnDismiss = false
                    dismiss()
                }
                is OriginalWebBottomSheet.Event.OpenTagScreen -> {
                    ItemsTaggingFragment.show(
                        activity,
                        event.item,
                        null
                    )
                }
                is OriginalWebBottomSheet.Event.ShowSavedToast -> {
                    Toast.makeText(context, getString(R.string.ts_add_added), Toast.LENGTH_SHORT).show()
                }
                is OriginalWebBottomSheet.Event.ShowArchivedToast -> {
                    Toast.makeText(context, getString(R.string.ts_item_archived), Toast.LENGTH_SHORT).show()
                }
                is OriginalWebBottomSheet.Event.ShowReAddedToast -> {
                    Toast.makeText(context, getString(R.string.ts_item_readded), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    companion object {
        private const val ARG_URL = "url"

        fun newInstance(url: String) = OriginalWebBottomSheetFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_URL, url)
            }
        }
    }
}
