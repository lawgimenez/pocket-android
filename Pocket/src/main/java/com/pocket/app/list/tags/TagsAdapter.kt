package com.pocket.app.list.tags

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.ideashower.readitlater.databinding.ViewNotTaggedItemBinding
import com.ideashower.readitlater.databinding.ViewTagItemBinding
import com.pocket.util.android.repeatOnCreated

class TagsAdapter(
    private val viewModel: TagBottomSheetViewModel,
    lifecycleOwner: LifecycleOwner,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    init {
        lifecycleOwner.repeatOnCreated {
            viewModel.tagsListUiState.collect {
                notifyDataSetChanged()
            }
        }
    }

    private val tags: List<BottomSheetItemUiState>
        get() = viewModel.tagsListUiState.value

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        if (viewType == ViewType.NOT_TAGGED.ordinal) {
            NotTaggedViewHolder(
                ViewNotTaggedItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false,
                )
            )
        } else {
            TagViewHolder(
                ViewTagItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false,
                )
            )
        }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is TagViewHolder -> (tags[position] as? TagBottomSheetItemUiState)?.let { holder.bind(it) }
            is NotTaggedViewHolder -> (tags[position] as? NotTaggedBottomSheetItemUiState)?.let { holder.bind(it) }
        }
    }

    override fun getItemCount(): Int = tags.size

    override fun getItemViewType(position: Int): Int {
        return if (tags[position] is NotTaggedBottomSheetItemUiState) {
            ViewType.NOT_TAGGED.ordinal
        } else {
            ViewType.TAG.ordinal
        }
    }

    inner class TagViewHolder(
        private val binding: ViewTagItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private var textChangeListener: TextWatcher? = null

        fun bind(state: TagBottomSheetItemUiState) {
            binding.tagText.removeTextChangedListener(textChangeListener)
            binding.tagText.setText(state.tag)
            binding.trashIcon.visibility = state.trashVisibility
            binding.tagText.isEnabled = state.editable
            binding.clickableView.visibility = if (state.editable) {
                View.GONE
            } else {
                View.VISIBLE
            }
            binding.trashIcon.setOnClickListener { viewModel.onDeleteTagClicked(state.tag) }

            binding.clickableView.setOnClickListener {
                viewModel.onTagClicked(state.tag)
            }

            textChangeListener = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int, ) {}
                override fun afterTextChanged(s: Editable?) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    viewModel.onTagEdited(state.tag, s.toString())
                }
            }
            binding.tagText.addTextChangedListener(textChangeListener)
        }
    }

    inner class NotTaggedViewHolder(
        private val binding: ViewNotTaggedItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(state: NotTaggedBottomSheetItemUiState) {
            binding.tagText.isEnabled = state.clickable
            binding.clickableView.visibility = if (state.clickable) {
                View.VISIBLE
            } else {
                View.GONE
            }

            binding.clickableView.setOnClickListener {
                viewModel.onNotTaggedClicked()
            }
        }
    }

    enum class ViewType {
        TAG,
        NOT_TAGGED
    }
}