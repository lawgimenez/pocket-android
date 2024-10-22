package com.pocket.sdk.util.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;

import com.ideashower.readitlater.R;
import com.pocket.app.help.Help;
import com.pocket.sdk.util.AbsPocketActivity;
import com.pocket.sdk.util.ErrorReport;
import com.pocket.util.android.ContextUtil;
import com.pocket.util.java.UserFacingErrorMessage;

import org.apache.commons.lang3.StringUtils;

/**
 * Helper for creating standardized alert dialogs Pocket messaging in the app.
 * All methods safely handle null or finishing activities and won't show in those cases.
 */
public class AlertMessaging {
	
	public static boolean isContextUnavailable(Context context) {
		if (context == null) return true;
		Activity activity = ContextUtil.findContext(context, Activity.class);
		return activity == null || activity.isFinishing();
	}
	
	/** Same as {@link #show(Context, CharSequence, CharSequence)}  where any string res ids that are 0 are set as null. */
	public static AlertDialog show(Context context, int title, int message) {
		return show(context, opt(context, title), opt(context, message));
	}
	
	/**
	 * Show an cancellable AlertDialog with an OK button that dismisses it.
	 * @param context The context, if null or finishing nothing will be shown.
	 * @param title null for no title, or the string resource.
	 * @param message null for no message, or the string resource.
	 * @return The dialog that was shown, or null if the context couldn't safely show the dialog
	 */
	public static AlertDialog show(Context context, CharSequence title, CharSequence message) {
		return show(context, title, message, null, null, opt(context, R.string.ac_ok), null, true);
	}
	
	/** Same as {@link #show(Context, int, int, int, DialogInterface.OnClickListener, int, DialogInterface.OnClickListener, boolean)} with cancelable set to true */
	public static AlertDialog show(Context context, int title, int message, int negativeButton, DialogInterface.OnClickListener negativeClick, int positiveButton, DialogInterface.OnClickListener positiveClick) {
		return show(context, title, message, negativeButton, negativeClick, positiveButton, positiveClick, true);
	}
	
	/** Same as {@link #show(Context, CharSequence, CharSequence, CharSequence, DialogInterface.OnClickListener, CharSequence, DialogInterface.OnClickListener, boolean)} where any string res ids that are 0 are set as null. */
	public static AlertDialog show(Context context, int title, int message, int negativeButton, DialogInterface.OnClickListener negativeClick, int positiveButton, DialogInterface.OnClickListener positiveClick, boolean cancellable) {
		return show(context, title != 0 ? context.getText(title) : null, message != 0 ? context.getText(message) : null, negativeButton != 0 ? context.getText(negativeButton) : null, negativeClick, positiveButton != 0 ? context.getText(positiveButton) : null, positiveClick, cancellable);
	}
	
	/**
	 * Shows an alert dialog
	 * @param title Optional title (null means no title)
	 * @param message Optional message (null means no message)
	 * @param negativeButton Optional negative button label (null means no negative button)
	 * @param negativeClick Optional negative click action (null just dismisses with no action)
	 * @param positiveButton Optional positive button label (null means no positive button)
	 * @param positiveClick Optional positive click action (null just dismisses with no action)
	 * @param cancellable whether or not to make the dialog cancellable (via outside touch or device back button)
	 * @return The dialog that was shown, or null if the context couldn't safely show the dialog
	 */
	public static AlertDialog show(Context context, CharSequence title, CharSequence message, CharSequence negativeButton, DialogInterface.OnClickListener negativeClick, CharSequence positiveButton, DialogInterface.OnClickListener positiveClick, boolean cancellable) {
		if (isContextUnavailable(context)) return null;
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		if (title != null) builder.setTitle(title);
		if (message != null) builder.setMessage(message);
		if (negativeButton != null) builder.setNegativeButton(negativeButton, negativeClick);
		if (positiveButton != null) builder.setPositiveButton(positiveButton, positiveClick);
		if (!cancellable) builder.setCancelable(cancellable);
		return builder.show();
	}
	
	/**
	 * Show an error dialog for an error from a task that requires an internet connection.
	 * If offline, it will show a "no connection" based error, if online it will use the {@link UserFacingErrorMessage} if available, then the provided message if set, and if neither is available, a generic fallback error message.
	 * Also see {@link #showError(AbsPocketActivity, CharSequence, CharSequence, boolean, DialogInterface.OnClickListener, ErrorReport, Help.Type)} for more details on the Get Help handling.
	 *
	 * @param activity Where to show the dialog. If null or finishing nothing will be shown
	 * @param error The error if known
	 * @param helpType The related {@link Help} constant to help give a subject line if they report or ask help with this error. Null will use a generic one.
	 * @param showGetHelp Not used if a connection error is shown. See {@link #showError(AbsPocketActivity, CharSequence, CharSequence, boolean, DialogInterface.OnClickListener, ErrorReport, Help.Type)} for more details on the Get Help handling.
	 * @param getHelpClick Not used if a connection error is shown. See {@link #showError(AbsPocketActivity, CharSequence, CharSequence, boolean, DialogInterface.OnClickListener, ErrorReport, Help.Type)} for more details on the Get Help handling.
	 * @param title != 0 to show a specific title, 0 for no title or a generic one to be used,
	 * @param message != 0 to set a fallback message to be shown if there is no {@link UserFacingErrorMessage}. If 0 a generic one will be used instead.
	 */
	public static void showConnectionDependantError(AbsPocketActivity activity, Throwable error, Help.Type helpType, boolean showGetHelp, DialogInterface.OnClickListener getHelpClick, int title, int message) {
		if (isContextUnavailable(activity)) return;
		
		if (activity.app().http().status().isOnline()) {
			CharSequence msg = UserFacingErrorMessage.find(error);
			msg = !StringUtils.isBlank(msg) ? msg : opt(activity, message);
			msg = !StringUtils.isBlank(msg) ? msg : activity.getText(R.string.dg_unexpected_m);
			showError(activity, opt(activity, title), msg, showGetHelp, getHelpClick, new ErrorReport(error, msg.toString()), helpType);

		} else {
			AlertMessaging.show(activity, title, R.string.dg_no_connection_m);
		}
	}
	
	/**
	 * Shows an error dialog with special handling related to requesting help from support.
	 * It will always have an OK button that dismisses the dialog and launches a support email on long press.
	 *
	 * @param context The activity to show it on, if null or finishing nothing is shown
	 * @param title The title to show or null for no title
	 * @param message The message to show or null for no message
	 * @param showGetHelp if true, a "Get Help" button as well
	 * @param getHelpClick (only used if showGetHelp is true and only applies to the button click, not the long press of OK) what to do when the "Get Help" button is clicked. If null it defaults to opening a support email.
	 * @param error An error report to pass along to {@link Help}
	 * @param helpType An type to pass along to {@link Help}, can be null
	 * @return The dialog that was shown, or null if the context couldn't safely show the dialog
	 */
	public static AlertDialog showError(AbsPocketActivity context, CharSequence title, CharSequence message, boolean showGetHelp, DialogInterface.OnClickListener getHelpClick, ErrorReport error, Help.Type helpType) {
		DialogInterface.OnClickListener openHelpEmail = (d,w) -> Help.requestHelp(helpType, error, context);
		DialogInterface.OnClickListener helpClick = showGetHelp ? (getHelpClick != null ? getHelpClick : openHelpEmail) : null;
		
		AlertDialog dialog = show(context, title, message, (showGetHelp ? opt(context, R.string.ac_get_help) : null), helpClick, opt(context, R.string.ac_ok), null, true);
		if (dialog == null) return null;
		
		// Hide launching a help email into long press of all OK buttons
		Runnable setupLongPress = () -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnLongClickListener(v -> {
			dialog.dismiss();
			askIfTheyWantToReport(context, error, helpType);
			return true;
		});
		if (dialog.isShowing()) {
			setupLongPress.run();
		} else {
			dialog.setOnShowListener(di -> setupLongPress.run());
		}
		return dialog;
	}
	
	/** Shows a dialog asking if they user wants to report the error. */
	public static AlertDialog askIfTheyWantToReport(Context context, ErrorReport error, Help.Type helpType) {
		return show(context, 0, R.string.dg_report_error_m, R.string.ac_no, null, R.string.ac_yes, (d,w) -> Help.requestHelp(helpType, error, context), true);
	}
	
	/** Returns the string if the context is available and the string id isn't 0. */
	private static CharSequence opt(Context context, int resId) {
		return context != null && resId != 0 ? context.getText(resId) : null;
	}

	/**
	 * Show a progress dialog
	 * @param context The context to show it on, if null or unavailable nothing will be shown
	 * @param message The message to display
	 * @param cancellable Whether or not it can be cancelled (such as using outside touch or back button)
	 * @return The shown dialog or null if it could not be displayed
	 */
	public static ProgressDialog progress(Context context, int message, boolean cancellable) {
		if (isContextUnavailable(context)) return null;
		ProgressDialog dialog = new ProgressDialog(context);
		dialog.setMessage(context.getText(message));
		dialog.setCancelable(cancellable);
		dialog.show();
		return dialog;
	}

	/**
	 * Attempts to dismiss the dialog but does not crash if the dialog's window is already detached.
	 * It is good practice for the activities and fragments that create dialogs to handle dismissing
	 * them properly as needed, but in some cases with async callbacks that isn't always easy to track.
	 * This can be used to safely dismiss if possible and ignore failures when not.
	 * @param dialog The dialog to dismiss. If null, this is a no-op
	 * @param context The context the dialog was shown on
	 */
	public static void dismissSafely(Dialog dialog, Context context) {
		if (dialog == null || !dialog.isShowing()) return;
		Activity activity = ContextUtil.findContext(context, Activity.class);
		if (activity != null && !activity.isFinishing()) {
			// Should be able to dismiss safely
			dialog.dismiss();
		} else {
			// Unsafe, but try anyways
			try {
				dialog.dismiss();
			} catch (Throwable ignore) {}
		}
	}

}
