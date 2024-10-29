package com.pocket.util.android;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.ideashower.readitlater.R;
import com.pocket.util.java.ListUtils;

import java.util.ArrayList;
import java.util.List;


public class Email {
	
	/**
	 * Will launch a SEND intent with the email info provided. If no app on the device
	 * can handle the intent then it will display an alert dialog to let the user know.
	 * 
	 * @param to
	 * @param subject
	 * @param message
	 * @param context
	 */
	public static void startEmailIntent(String to, String subject, String message, Context context, List<Attachment> attachments) {
		Intent intent;
        if (!ListUtils.isEmpty(attachments)) {
            intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
            ArrayList<Uri> uris = new ArrayList<>(attachments.size());
            for (Attachment file : attachments) {
                uris.add(file.uri);
            }
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);

        } else {
            intent = new Intent(Intent.ACTION_SEND);
        }

        intent.setType("text/plain");
		
		intent.putExtra(Intent.EXTRA_EMAIL, new String[]{to});
		intent.putExtra(Intent.EXTRA_SUBJECT, subject);
		if (message != null) {
			intent.putExtra(Intent.EXTRA_TEXT, message);
		}
		
		if (IntentUtils.isActivityIntentAvailable(context, intent)) {
			context.startActivity(intent);
			
		} else {
            String msg = context.getString(R.string.dg_no_email_app_m) + "\n\n" + context.getString(R.string.dg_no_email_app_m_support);

			new AlertDialog.Builder(context)
				.setTitle(R.string.dg_no_email_app_t)
				.setMessage(msg)
				.setPositiveButton(R.string.ac_ok, null)
				.show();
		}
	}

    public static class Attachment {

        public final String mimeType;
        public final Uri uri;

        public Attachment(String mimeType, String uri) {
            this(mimeType, Uri.parse(uri));
        }

        public Attachment(String mimeType, Uri uri) {
            this.mimeType = mimeType;
            this.uri = uri;
        }

    }

}
