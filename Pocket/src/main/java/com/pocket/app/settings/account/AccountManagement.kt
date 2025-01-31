package com.pocket.app.settings.account

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Annotation
import android.text.SpannedString
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.text.buildSpannedString
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ideashower.readitlater.R
import com.ideashower.readitlater.databinding.ActivityDeleteAccountBinding
import com.pocket.analytics.Tracker
import com.pocket.analytics.appevents.SettingsEvents
import com.pocket.app.App
import com.pocket.app.UserManager
import com.pocket.app.settings.AbsPrefsFragment
import com.pocket.app.settings.view.preferences.Preference
import com.pocket.app.settings.view.preferences.PreferenceViews
import com.pocket.repository.UserRepository
import com.pocket.sdk.util.AbsPocketActivity
import com.pocket.sdk.util.AbsPocketFragment
import com.pocket.sdk.util.dialog.AlertMessaging
import com.pocket.sync.await
import com.pocket.sync.source.result.SyncException
import com.pocket.ui.text.ThemedClickableSpan
import com.pocket.ui.text.ThemedClickableSpan.StateSource
import com.pocket.ui.view.themed.ThemedTextView
import com.pocket.util.android.FormFactor
import com.pocket.util.android.fragment.FragmentUtil.FragmentLaunchMode
import com.pocket.util.android.fragment.FragmentUtil.addFragmentAsDialog
import com.pocket.util.collectWhenResumed
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** A thin class to allow [AccountManagementFragment] to be launched as a fullscreen activity. */
@AndroidEntryPoint
class AccountManagementActivity : AbsPocketActivity() {
    companion object {
        fun startActivity(context: Context) {
            context.startActivity(newStartIntent(context))
        }

        fun newStartIntent(context: Context?): Intent {
            return Intent(context, AccountManagementActivity::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            // New instance
            val frag: Fragment = AccountManagementFragment.newInstance()
            setContentFragment(frag, null, AccountManagementFragment.getLaunchMode(this))
        } else {
            // Fragment is restored
        }
    }

    override fun getAccessType(): ActivityAccessRestriction {
        return ActivityAccessRestriction.REQUIRES_LOGIN
    }
}

@AndroidEntryPoint
class AccountManagementFragment : AbsPrefsFragment() {
    companion object {
        fun getLaunchMode(context: Context): FragmentLaunchMode {
            return if (FormFactor.showSecondaryScreensInDialogs(context)) {
                FragmentLaunchMode.DIALOG
            } else {
                FragmentLaunchMode.ACTIVITY
            }
        }

        fun newInstance(): AccountManagementFragment = AccountManagementFragment()

        fun show(activity: FragmentActivity, mode: FragmentLaunchMode?) {
            if ((mode ?: getLaunchMode(activity)) == FragmentLaunchMode.DIALOG) {
                addFragmentAsDialog(newInstance(), activity)
            } else {
                AccountManagementActivity.startActivity(activity)
            }
        }
    }

    @Inject lateinit var tracker: Tracker

    override fun onViewCreatedImpl(view: View, savedInstanceState: Bundle?) {
        super.onViewCreatedImpl(view, savedInstanceState)
        tracker.track(SettingsEvents.accountManagementImpression())
    }

    override fun getTitle(): Int = R.string.setting_account_management

    override fun createPrefs(prefs: ArrayList<Preference>) {
        // Delete account.
        prefs.add(
            PreferenceViews.newActionBuilder(this, R.string.setting_delete_account)
                .setOnClickListener {
                    tracker.track(SettingsEvents.deleteAccountRowClicked())
                    DeleteAccountFragment.show(absPocketActivity)
                }
                .build()
        )
    }

    override fun getBannerView(): View? = null
}

/** A thin class to allow [DeleteAccountFragment] to be launched as a fullscreen activity. */
@AndroidEntryPoint
class DeleteAccountActivity : AbsPocketActivity() {
    companion object {
        fun startActivity(context: Context) {
            context.startActivity(newStartIntent(context))
        }

        fun newStartIntent(context: Context?): Intent {
            return Intent(context, DeleteAccountActivity::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            // New instance
            val frag: Fragment = DeleteAccountFragment.newInstance()
            setContentFragment(frag, null, DeleteAccountFragment.getLaunchMode(this))
        } else {
            // Fragment is restored
        }
    }

    override fun getAccessType(): ActivityAccessRestriction {
        return ActivityAccessRestriction.REQUIRES_LOGIN
    }
}

@AndroidEntryPoint
class DeleteAccountFragment : AbsPocketFragment() {
    companion object {
        fun getLaunchMode(context: Context): FragmentLaunchMode {
            return if (FormFactor.showSecondaryScreensInDialogs(context)) {
                FragmentLaunchMode.DIALOG
            } else {
                FragmentLaunchMode.ACTIVITY
            }
        }

        fun newInstance(): DeleteAccountFragment = DeleteAccountFragment()

        fun show(activity: FragmentActivity, mode: FragmentLaunchMode? = null) {
            if ((mode ?: getLaunchMode(activity)) == FragmentLaunchMode.DIALOG) {
                addFragmentAsDialog(newInstance(), activity)
            } else {
                DeleteAccountActivity.startActivity(activity)
            }
        }
    }

    @Inject lateinit var tracker: Tracker

    private var _binding: ActivityDeleteAccountBinding? = null
    private val binding: ActivityDeleteAccountBinding
        get() = _binding!!
    private val viewModel: DeleteAccountViewModel by viewModels()

    private var progressDialog: ProgressDialog? = null

    override fun onCreateViewImpl(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedInstanceState: Bundle?,
    ): View {
        _binding = ActivityDeleteAccountBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        return binding.root
    }

    override fun onViewCreatedImpl(view: View, savedInstanceState: Bundle?) {
        super.onViewCreatedImpl(view, savedInstanceState)

        viewModel.onInitialized()
        setupViews()
        setupEventListener()
        setupProgressDialog()

        tracker.track(SettingsEvents.deleteAccountConfirmationImpression())
    }

    private fun setupViews() {
        with(binding) {
            appBar.bind()
                .onLeftIconClick {
                    tracker.track(SettingsEvents.deleteDismissed())
                    finish()
                }

            with(cancelPremiumLabel) {
                applyAnnotations()
                setMovementMethodForLinks(true)
            }

            cancelPremiumCheckbox.setOnCheckedChangeListener { _, isChecked ->
                viewModel?.onCancelPremiumConfirmed(isChecked)
            }
            permanentlyDeleteCheckbox.setOnCheckedChangeListener { _, isChecked ->
                viewModel?.onPermanentlyDeleteConfirmed(isChecked)
            }

            cancelButton.setOnClickListener {
                tracker.track(SettingsEvents.deleteDismissed())
                finish()
            }
        }
    }

    private fun ThemedTextView.applyAnnotations() {
        val original = text as SpannedString
        val annotations = original.getSpans(0, original.length, Annotation::class.java)
        text = buildSpannedString {
            append(original)
            for (annotation in annotations) {
                if (annotation.key == "link") {
                    val link = object : ThemedClickableSpan(
                        ContextCompat.getColorStateList(
                            requireContext(),
                            com.pocket.ui.R.color.pkt_themed_teal_2_clickable,
                        )!!,
                        StateSource { drawableState }
                    ) {
                        override fun onClick(view: View) {
                            App.viewUrl(requireContext(), annotation.value)
                        }
                    }
                    setSpan(
                        link,
                        original.getSpanStart(annotation),
                        original.getSpanEnd(annotation),
                        original.getSpanFlags(annotation),
                    )
                }
            }
        }
    }

    private fun setupEventListener() {
        viewModel.events.collectWhenResumed(viewLifecycleOwner) {
            when (it) {
                DeleteAccountViewModel.ShowDeleteAccountError -> {
                    Toast.makeText(
                        requireContext(),
                        R.string.setting_delete_account_error,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun setupProgressDialog() {
        val context = requireContext()
        viewModel.uiState
            .map { it.deleteAccountSpinnerVisible }
            .distinctUntilChanged()
            .collectWhenResumed(viewLifecycleOwner) { visible ->
                if (visible) {
                    progressDialog = ProgressDialog.show(
                        context,
                        null,
                        getStringSafely(R.string.setting_delete_account_in_progress),
                        true,
                        false,
                    )
                } else {
                    AlertMessaging.dismissSafely(progressDialog, context)
                    progressDialog = null
                }
            }
    }
}

@HiltViewModel
class DeleteAccountViewModel
@Inject constructor(
    private val userRepository: UserRepository,
    private val userManager: UserManager,
    private val tracker: Tracker,
): ViewModel() {
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState>
        get() = _uiState

    private val _events = MutableSharedFlow<Event>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events: SharedFlow<Event>
        get() = _events

    fun onInitialized() {
        setupLoginInfoFlow()
    }

    fun onCancelPremiumConfirmed(confirmed: Boolean) {
        _uiState.update { it.copy(cancelPremiumConfirmed = confirmed) }
    }

    fun onPermanentlyDeleteConfirmed(confirmed: Boolean) {
        _uiState.update { it.copy(permanentlyDeletedConfirmed = confirmed) }
    }

    fun onDeleteButtonClicked() {
        tracker.track(SettingsEvents.deleteConfirmationClicked())
        _uiState.update { it.copy(deleteAccountSpinnerVisible = true) }
        viewModelScope.launch {
            try {
                userManager.deleteAccount().await()
            } catch (e: SyncException) {
                _events.tryEmit(ShowDeleteAccountError)
                e.printStackTrace()
            } finally {
                _uiState.update { it.copy(deleteAccountSpinnerVisible = false) }
            }
        }
    }

    private fun setupLoginInfoFlow() {
        viewModelScope.launch {
            userRepository.getLoginInfoAsFlow().collect { loginInfo ->
                val hasPremium = loginInfo.account?.premium_status ?: false
                _uiState.update { it.copy(cancelPremiumCheckBoxVisible = hasPremium) }
            }
        }
    }

    data class UiState(
        val cancelPremiumCheckBoxVisible: Boolean = false,
        val cancelPremiumConfirmed: Boolean = false,
        val permanentlyDeletedConfirmed: Boolean = false,
        val deleteAccountSpinnerVisible: Boolean = false,
    ) {
        val deleteButtonEnabled: Boolean get() {
            return (cancelPremiumConfirmed || !cancelPremiumCheckBoxVisible) &&
                    permanentlyDeletedConfirmed
        }
    }

    sealed class Event
    object ShowDeleteAccountError : Event()
}
