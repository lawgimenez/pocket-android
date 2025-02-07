package com.pocket.app;

import android.os.Bundle;
import android.text.InputFilter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.ideashower.readitlater.R;
import com.pocket.app.settings.Theme;
import com.pocket.sdk.offline.cache.AssetUser;
import com.pocket.sdk.util.AbsPocketActivity;
import com.pocket.sdk2.view.LazyAssetBitmap;
import com.pocket.ui.util.DimenUtil;
import com.pocket.ui.util.LazyBitmap;
import com.pocket.ui.util.PlaceHolderBuilder;
import com.pocket.ui.util.PocketUIViewUtil;
import com.pocket.ui.view.AppBar;
import com.pocket.ui.view.PaletteView;
import com.pocket.ui.view.info.InfoPage;
import com.pocket.ui.view.info.InfoPageAdapter;
import com.pocket.ui.view.info.InfoPagingView;
import com.pocket.ui.view.item.ItemRowView;
import com.pocket.ui.view.menu.MenuItem;
import com.pocket.ui.view.menu.ThemedPopupMenu;
import com.pocket.ui.view.notification.ItemSnackbarView;
import com.pocket.ui.view.notification.PktSnackbar;
import com.pocket.ui.view.progress.skeleton.row.SkeletonItemRow;
import com.pocket.ui.view.settings.SettingsImportantButton;
import com.pocket.util.android.FormFactor;
import com.pocket.util.android.ViewUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * TODO Documentation
 */
public class PocketUiPlaygroundActivity extends AbsPocketActivity {
	
	private int[] overrideTheme;
	private boolean isAllDisabled;
	private int itemImagesState = 0;
	private int itemMode = 0;

	private ViewGroup fullscreenView;

	@Override
	protected ActivityAccessRestriction getAccessType() {
		return ActivityAccessRestriction.ANY;
	}
	
	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (!app().mode().isForInternalCompanyOnly()) {
			finish();
			return;
		}
		setContentView(com.pocket.ui.R.layout.pocket_ui_playground);
		bindExamples();
	}

	private void showFullscreen(View view) {
		fullscreenView.removeAllViews();
		fullscreenView.addView(view);
	}
	
	private void bindExamples() {

		fullscreenView = findViewById(com.pocket.ui.R.id.fullscreen);

		AppBar appBar = findViewById(com.pocket.ui.R.id.app_bar);
		appBar.bind().clear().onLeftIconClick(v -> finish()).title("UI Component Playground").addIconAction(
				com.pocket.ui.R.drawable.ic_pkt_settings_line, com.pocket.ui.R.string.ic_filters, v -> {
			// Filters
			int themeSelected = 0;
			if (overrideTheme == Theme.STATE_DARK) {
				themeSelected = 1;
			}
			new ThemedPopupMenu(v.getContext(),
					ThemedPopupMenu.Section.radio("Theme", themeSelected, Arrays.asList(
							new MenuItem(com.pocket.ui.R.string.light, 0, view -> setThemeOverride(Theme.STATE_LIGHT, Theme.LIGHT)),
							new MenuItem(com.pocket.ui.R.string.dark, 0, view -> setThemeOverride(Theme.STATE_DARK, Theme.DARK)))),
					ThemedPopupMenu.Section.radio("Interactions", isAllDisabled ? 1 : 0, Arrays.asList(
							new MenuItem(R.string.dev_enabled, 0, view -> setEnabledOverride(true)),
							new MenuItem(R.string.dev_disabled, 0, view -> setEnabledOverride(false)))),
					ThemedPopupMenu.Section.radio("Item Images", itemImagesState, Arrays.asList(
							new MenuItem(R.string.dev_images, 0, view -> setItemImagesState(0)),
							new MenuItem(R.string.dev_no_images, 0, view -> setItemImagesState(1)),
							new MenuItem(R.string.dev_placeholders, 0, view -> setItemImagesState(2)))),
					ThemedPopupMenu.Section.radio("Item Mode", itemMode, Arrays.asList(
							new MenuItem(R.string.dev_normal, 0, view -> setItemMode(0)),
							new MenuItem(R.string.dev_edit, 0, view -> setItemMode(1))))
			).show(v);
		});

		LazyBitmap image = new LazyAssetBitmap("https://apod.nasa.gov/apod/image/1808/SoulNebula_Vargas_960.jpg", AssetUser.forSession());
		if (itemImagesState == 1) {
			image = null;
		} else if (itemImagesState == 2) {
			image = new LazyAssetBitmap("https://apod.nasa.gov/apod/image/1808/SoulNebula_Vargas_960-broken-url-that-will-never-load.jpg", AssetUser.forSession());
		}
		
		// Palettes
		PaletteView view = findViewById(com.pocket.ui.R.id.palette);
		view.clearRows();

		if (overrideTheme == Theme.STATE_DARK) {
			view.addRow(com.pocket.ui.R.color.pkt_dm_base_bg, com.pocket.ui.R.color.pkt_dm_grey_6,
					com.pocket.ui.R.color.pkt_dm_grey_5, com.pocket.ui.R.color.pkt_dm_grey_4, com.pocket.ui.R.color.pkt_dm_grey_3, com.pocket.ui.R.color.pkt_dm_grey_2, com.pocket.ui.R.color.pkt_dm_grey_1);
			view.addRow(com.pocket.ui.R.color.pkt_dm_teal_6, com.pocket.ui.R.color.pkt_dm_teal_5,
					com.pocket.ui.R.color.pkt_dm_teal_4, com.pocket.ui.R.color.pkt_dm_teal_3, com.pocket.ui.R.color.pkt_dm_teal_2, com.pocket.ui.R.color.pkt_dm_teal_1);
			view.addRow(com.pocket.ui.R.color.pkt_dm_coral_5, com.pocket.ui.R.color.pkt_dm_coral_4, com.pocket.ui.R.color.pkt_dm_coral_3, com.pocket.ui.R.color.pkt_dm_coral_2, com.pocket.ui.R.color.pkt_dm_coral_1);
			view.addRow(com.pocket.ui.R.color.amber_80, com.pocket.ui.R.color.amber_60, com.pocket.ui.R.color.pkt_dm_amber_3, com.pocket.ui.R.color.amber_30, com.pocket.ui.R.color.amber_30);
			view.addRow(com.pocket.ui.R.color.pkt_dm_lapis_5, com.pocket.ui.R.color.pkt_dm_lapis_3);
			view.addRow(com.pocket.ui.R.color.pkt_dm_apricot_1);
		} else {
			view.addRow(com.pocket.ui.R.color.pkt_base_bg, com.pocket.ui.R.color.pkt_grey_6, com.pocket.ui.R.color.pkt_grey_5, com.pocket.ui.R.color.pkt_grey_4, com.pocket.ui.R.color.pkt_grey_3, com.pocket.ui.R.color.pkt_grey_2, com.pocket.ui.R.color.pkt_grey_1);
			view.addRow(com.pocket.ui.R.color.pkt_teal_6, com.pocket.ui.R.color.pkt_teal_5, com.pocket.ui.R.color.pkt_teal_4, com.pocket.ui.R.color.pkt_teal_3, com.pocket.ui.R.color.pkt_teal_2, com.pocket.ui.R.color.pkt_teal_1);
			view.addRow(com.pocket.ui.R.color.pkt_coral_5, com.pocket.ui.R.color.pkt_coral_4, com.pocket.ui.R.color.pkt_coral_3, com.pocket.ui.R.color.pkt_coral_2, com.pocket.ui.R.color.pkt_coral_1);
			view.addRow(com.pocket.ui.R.color.amber_5, com.pocket.ui.R.color.pkt_amber_4, com.pocket.ui.R.color.pkt_amber_3, com.pocket.ui.R.color.amber_60, com.pocket.ui.R.color.amber_30);
			view.addRow(com.pocket.ui.R.color.pkt_lapis_5, com.pocket.ui.R.color.pkt_lapis_3);
			view.addRow(com.pocket.ui.R.color.pkt_apricot_1);
		}

		final String discTitle = "A Simple Way to Map Out your Career Ambitions";
		final String discDomain = "Ted Ideas";
		final String discEst = "6 min";

		ItemRowView discItem1 = findViewById(com.pocket.ui.R.id.discover_item1);
		discItem1.bind().clear()
				.thumbnail(image, false)
				.meta()
				.title(discTitle)
				.domain(discDomain)
				.timeEstimate(discEst);

		// skeleton views
		// swap out the current one with a new one, refreshing all the random elements of the skeleton views / paragraphs
		findViewById(com.pocket.ui.R.id.skeleton1).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v1) {
				View row = new SkeletonItemRow(PocketUiPlaygroundActivity.this);
				row.setOnClickListener(this);
				PocketUIViewUtil.replaceView(v1, row);
			}
		});

		// placeholder images
		View.OnClickListener placeholderlistener = v -> {
			LinearLayout form = new LinearLayout(this);
			form.setOrientation(LinearLayout.VERTICAL);
			EditText characterView = new EditText(PocketUiPlaygroundActivity.this);
			characterView.setFilters(new InputFilter[]{new InputFilter.LengthFilter(1)});
			characterView.setHint("character");
			characterView.setText("A");
			RadioGroup colorsGroup = new RadioGroup(this);
			for (PlaceHolderBuilder.PktColor colorval :PlaceHolderBuilder.PktColor.values()) {
				android.widget.RadioButton c = new android.widget.RadioButton(this);
				c.setText(colorval.name());
				c.setId(colorval.ordinal());
				colorsGroup.addView(c);
			}
			((android.widget.RadioButton)colorsGroup.getChildAt(0)).setChecked(true);
			RadioGroup cornersGroup = new RadioGroup(this);
			cornersGroup.setPadding(0, DimenUtil.dpToPxInt(this, 20), 0, 0);
			for (PlaceHolderBuilder.Corner cornerval :PlaceHolderBuilder.Corner.values()) {
				android.widget.RadioButton c = new android.widget.RadioButton(this);
				c.setText(cornerval.name());
				c.setId(cornerval.ordinal());
				cornersGroup.addView(c);
			}
			((android.widget.RadioButton)cornersGroup.getChildAt(0)).setChecked(true);
			form.addView(characterView);
			form.addView(colorsGroup);
			form.addView(cornersGroup);
			new AlertDialog.Builder(PocketUiPlaygroundActivity.this).setView(form).setPositiveButton("ok", (dialog, which) -> {
				if (characterView.getText().length() != 0) {
					((ImageView) v).setImageDrawable(PlaceHolderBuilder.getDrawable(PocketUiPlaygroundActivity.this, characterView.getText().charAt(0), PlaceHolderBuilder.PktColor.values()[colorsGroup.indexOfChild(colorsGroup.findViewById(colorsGroup.getCheckedRadioButtonId()))], PlaceHolderBuilder.Corner.values()[cornersGroup.indexOfChild(cornersGroup.findViewById(cornersGroup.getCheckedRadioButtonId()))]));
				}
			}).show();
		};
		ImageView ph1 = findViewById(com.pocket.ui.R.id.ph1);
		ImageView ph2 = findViewById(com.pocket.ui.R.id.ph2);
		ImageView ph3 = findViewById(com.pocket.ui.R.id.ph3);
		ImageView ph4 = findViewById(com.pocket.ui.R.id.ph4);
		ph1.setImageDrawable(PlaceHolderBuilder.getDrawable(PocketUiPlaygroundActivity.this, 'A', PlaceHolderBuilder.PktColor.BLUE, PlaceHolderBuilder.Corner.TOP_LEFT));
		ph2.setImageDrawable(PlaceHolderBuilder.getDrawable(PocketUiPlaygroundActivity.this, 'A', PlaceHolderBuilder.PktColor.TEAL, PlaceHolderBuilder.Corner.TOP_RIGHT));
		ph3.setImageDrawable(PlaceHolderBuilder.getDrawable(PocketUiPlaygroundActivity.this, 'A', PlaceHolderBuilder.PktColor.CORAL, PlaceHolderBuilder.Corner.BOTTOM_LEFT));
		ph4.setImageDrawable(PlaceHolderBuilder.getDrawable(PocketUiPlaygroundActivity.this, 'A', PlaceHolderBuilder.PktColor.AMBER, PlaceHolderBuilder.Corner.BOTTOM_RIGHT));
		ph1.setOnClickListener(placeholderlistener);
		ph2.setOnClickListener(placeholderlistener);
		ph3.setOnClickListener(placeholderlistener);
		ph4.setOnClickListener(placeholderlistener);

		View.OnClickListener onClick = v -> Toast.makeText(this, "Tapped " + v, Toast.LENGTH_LONG).show();

		ItemSnackbarView itemSnackbar = findViewById(com.pocket.ui.R.id.item_snackbar);
		itemSnackbar.bind().clear()
				.onClick(onClick)
				.thumbnail(image)
				.featureTitle("CONTINUE READING")
				.meta()
				.title("An article with a title: this is it right here")
				.domain("pocket.com")
				.timeEstimate("30 min");

		// onboarding "learn more" example
		InfoPagingView learnmore = new InfoPagingView(this);
		List<InfoPage> learnmorepages = new ArrayList<>();

		String buttonText = getString(R.string.ac_continue);
		View.OnClickListener buttonListener = v -> learnmore.bind().nextPage();
		learnmorepages.add(new InfoPage(R.drawable.pkt_onboarding_treasure, getString(R.string.onboarding_learn_more_1_title), getString(R.string.onboarding_learn_more_1_text), buttonText, null, buttonListener));
		learnmorepages.add(new InfoPage(R.drawable.pkt_onboarding_treasure, getString(R.string.onboarding_learn_more_2_title), getString(R.string.onboarding_learn_more_2_text), buttonText, null, buttonListener));
		learnmorepages.add(new InfoPage(R.drawable.pkt_onboarding_treasure, getString(R.string.onboarding_learn_more_3_title), getString(R.string.onboarding_learn_more_3_text), buttonText, null, buttonListener));
		learnmore.bind().adapter(new InfoPageAdapter(this, FormFactor.getWindowWidthPx(this), learnmorepages)).header(R.drawable.pkt_onboarding_logo);

		findViewById(com.pocket.ui.R.id.infoPagingView).setOnClickListener(v -> showFullscreen(learnmore));

		
		findViewById(com.pocket.ui.R.id.defaultButton).setOnClickListener(v -> {
			PktSnackbar.make(this, PktSnackbar.Type.DEFAULT, "Hello!", null, R.string.ac_ok, null).show();
		});
        findViewById(com.pocket.ui.R.id.errorButton).setOnClickListener(v -> {
            PktSnackbar.make(this, PktSnackbar.Type.ERROR_EXCLAIM, "Error Text!", null, R.string.ac_ok, null).show();
        });

        SettingsImportantButton logout = findViewById(com.pocket.ui.R.id.logOut);
		logout.bind().text("Log Out");
		logout.setOnClickListener(v -> PktSnackbar.make(this, PktSnackbar.Type.ERROR_DISMISSABLE, "Logged Out!", null).show());

		AppBar appBar1 = findViewById(com.pocket.ui.R.id.appbar1);
		appBar1.bind()
				.onLeftIconClick(v -> finish())
				.addIconAction(com.pocket.ui.R.drawable.ic_pkt_listen_line, com.pocket.ui.R.string.ic_listen, v -> Toast.makeText(this, "Clicked 1", Toast.LENGTH_LONG).show());
		AppBar appBar3 = findViewById(com.pocket.ui.R.id.appbar3);
		appBar3.bind()
				.addIconAction(com.pocket.ui.R.drawable.ic_pkt_listen_line, com.pocket.ui.R.string.ic_listen, v -> Toast.makeText(this, "Clicked 1", Toast.LENGTH_LONG).show())
				.addIconAction(com.pocket.ui.R.drawable.ic_pkt_add_tags_line, com.pocket.ui.R.string.ic_listen, v -> Toast.makeText(this, "Clicked 2", Toast.LENGTH_LONG).show())
				.addIconAction(com.pocket.ui.R.drawable.ic_pkt_android_overflow_solid, com.pocket.ui.R.string.ic_overflow, v -> Toast.makeText(this, "Clicked 3", Toast.LENGTH_LONG).show());

	}

	private void setItemImagesState(int state) {
		itemImagesState = state;
		bindExamples();
	}
	
	private void setItemMode(int state) {
		itemMode = state;
	}
	
	private void setThemeOverride(int[] override, int theme) {
		onThemeChanged(theme);
		overrideTheme = override;
		ViewUtil.refreshDrawableStateDeep(mRoot.getRootView());
		bindExamples();
	}
	
	private void setEnabledOverride(boolean enable) {
		isAllDisabled = !enable;
		setDeepEnabled(findViewById(com.pocket.ui.R.id.components), enable);
	}
	
	private void setDeepEnabled(View view, boolean enable) {
		if (view == null) {
			return;
		}
		
		view.setEnabled(enable);
		if (view instanceof ViewGroup) {
			ViewGroup vg = (ViewGroup) view;
			for (int i = 0; i < vg.getChildCount(); i++) {
				setDeepEnabled(vg.getChildAt(i), enable);
			}
		}
	}
	
	@Override
	public int[] getThemeState(@NonNull View view) {
		if (overrideTheme != null) {
			return overrideTheme;
		} else {
			return super.getThemeState(view);
		}
	}

	@Override
	public void onBackPressed() {
		if (fullscreenView.getChildCount() > 0) {
			fullscreenView.removeAllViews();
		} else {
			super.onBackPressed();
		}
	}
	
}
