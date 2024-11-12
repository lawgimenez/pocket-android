package com.pocket.ui.view.menu;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.PopupWindow;

import com.pocket.ui.R;
import com.pocket.ui.util.DimenUtil;
import com.pocket.ui.view.themed.ThemedRecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class ThemedPopupMenu {
	
	private static final int VIEW_TYPE_HEADER = 0;
	private static final int VIEW_TYPE_RADIO_OPTION = 1;
	private static final int VIEW_TYPE_ACTION_OPTION = 2;
	
	private final RecyclerView view;
	/** CharSequences as headers, MenuItem as options. */
	private final List<Object> rows = new ArrayList<>();
	private final Map<MenuItem, MenuType> types = new HashMap<>();
	private final Set<MenuItem> selected = new HashSet<>();
	private PopupWindow window;
	
	public ThemedPopupMenu(Context context, Section... sections) {
		for (Section section : sections) {
			if (section.label != null) {
				rows.add(section.label);
			}
			for (MenuItem option : section.options) {
				rows.add(option);
				types.put(option, section.type);
			}
			if (section.selectedPosition >= 0) {
				selected.add(section.options.get(section.selectedPosition));
			}
		}
		
		view = new ThemedRecyclerView(context);
		view.setBackgroundResource(R.drawable.cl_pkt_popup_bg);
		view.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
		view.setAdapter(new RecyclerView.Adapter() {
			
			@Override
			public int getItemViewType(int position) {
				if (rows.get(position) instanceof CharSequence) {
					return VIEW_TYPE_HEADER;
				} else {
					MenuItem option = (MenuItem) rows.get(position);
					switch (types.get(option)) {
						case RADIO: return VIEW_TYPE_RADIO_OPTION;
						case ACTIONS: return VIEW_TYPE_ACTION_OPTION;
					}
				}
				throw new RuntimeException("unknown type at position " + position);
			}
			
			@Override
			public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
				switch (viewType) {
					case VIEW_TYPE_HEADER:
						return new HeaderHolder(context);
					case VIEW_TYPE_RADIO_OPTION:
						return new RadioOptionHolder(context);
					case VIEW_TYPE_ACTION_OPTION:
						return new ActionOptionHolder(context);
				}
				return null;
			}
			
			@Override
			public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
				if (holder instanceof HeaderHolder) {
					((HeaderHolder) holder).bind((CharSequence) rows.get(position));
				} else if (holder instanceof RadioOptionHolder) {
					MenuItem option = (MenuItem) rows.get(position);
					((RadioOptionHolder) holder).bind(option, selected.contains(option));
				} else if (holder instanceof ActionOptionHolder) {
					MenuItem option = (MenuItem) rows.get(position);
					((ActionOptionHolder) holder).bind(option);
				}
			}
			
			@Override
			public int getItemCount() {
				return rows.size();
			}
		});
		
		view.setMinimumWidth(DimenUtil.dpToPxInt(context, 200));
	}

	public void show(View anchor) {
		dismiss();
		window = new PopupWindow(view.getContext());
		window.setBackgroundDrawable(ContextCompat.getDrawable(anchor.getContext(), R.drawable.pkt_popup_bg));
		window.setFocusable(true);
		window.setContentView(view);
		window.setWidth(WindowManager.LayoutParams.WRAP_CONTENT);
		window.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
        window.showAsDropDown(anchor, -anchor.getWidth(), -anchor.getHeight());
	}
	
	private void dismiss() {
		if (window != null) {
			window.dismiss();
			window = null;
		}
	}
	
	private static class HeaderHolder extends RecyclerView.ViewHolder {
		public HeaderHolder(Context context) {
			super(new SectionHeaderView(context));
			itemView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		}
		
		public void bind(int stringResId) {
			((SectionHeaderView) itemView).bind().clear()
					.showBottomDivider(false)
					.label(stringResId);
		}
		
		public void bind(CharSequence label) {
			((SectionHeaderView) itemView).bind().clear()
				.showBottomDivider(false)
				.label(label);
		}
	}
	
	private class RadioOptionHolder extends RecyclerView.ViewHolder {
		public RadioOptionHolder(Context context) {
			super(new RadioOptionRowView(context));
			itemView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		}
		public void bind(MenuItem option, boolean selected) {
			RadioOptionRowView view = ((RadioOptionRowView) itemView);
			view.setLabel(option.label);
			view.setOnClickListener(v -> {
				dismiss();
				option.onClick(v);
			});
			view.setChecked(selected);
			view.setEnabled(option.isEnabled());
		}
	}
	
	private class ActionOptionHolder extends RecyclerView.ViewHolder {
		public ActionOptionHolder(Context context) {
			super(new OptionRowView(context));
			itemView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		}
		public void bind(MenuItem option) {
			OptionRowView view = ((OptionRowView) itemView);
			view.setUiEntityIdentifier(option.uiEntityIdentifier);
			view.setLabel(option.label);
			view.setIcon(option.icon);
			view.setOnClickListener(v -> {
				dismiss();
				option.onClick(v);
			});
			view.setEnabled(option.isEnabled());
		}
	}
	
	public enum MenuType  {
		ACTIONS,
		RADIO
	}
	
	public static class Section {
		private final MenuType type;
		private final CharSequence label;
		private final List<MenuItem> options;
		private final int selectedPosition;
		
		public static Section actions(CharSequence label, List<MenuItem> options) {
			return new Section(MenuType.ACTIONS, label, -1, options);
		}
		
		public static Section radio(CharSequence label, int selectedPosition, List<MenuItem> options) {
			return new Section(MenuType.RADIO, label, selectedPosition, options);
		}
		
		private Section(MenuType type, CharSequence label, int selectedPosition, List<MenuItem> options) {
			this.type = type;
			this.options = options;
			this.label = label;
			this.selectedPosition = selectedPosition;
		}
	}
	
}
