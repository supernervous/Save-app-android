package io.scal.secureshareui.controller;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URLEncoder;
import java.util.HashMap;

import io.scal.secureshareui.lib.Util;
import io.scal.secureshareui.login.ArchiveLoginActivity;
import io.scal.secureshareui.model.Account;

public class ArchiveSiteController extends SiteController {

	public static final String SITE_NAME = "Internet Archive";
	public static final String SITE_KEY = "archive";
	private static final String TAG = "ArchiveSiteController";
    static {
        METADATA_REQUEST_CODE = 1022783271;
    }

	private static final String ARCHIVE_API_ENDPOINT = "https://s3.us.archive.org";
	public static final MediaType MEDIA_TYPE = MediaType.parse("");

	public ArchiveSiteController(Context context, Handler handler, String jobId) {
		super(context, handler, jobId);
	}

	@Override
	public void startRegistration(Account account) {
		Intent intent = new Intent(mContext, ArchiveLoginActivity.class);
		intent.putExtra("register",true);
		intent.putExtra(SiteController.EXTRAS_KEY_CREDENTIALS, account.getCredentials());
		((Activity) mContext).startActivityForResult(intent, SiteController.CONTROLLER_REQUEST_CODE);
		// FIXME not a safe cast, context might be a service
	}

	@Override
	public void startAuthentication(Account account) {
		Intent intent = new Intent(mContext, ArchiveLoginActivity.class);
		intent.putExtra(SiteController.EXTRAS_KEY_CREDENTIALS, account.getCredentials());
		intent.putExtra("useTor",mUseTor);
		((Activity) mContext).startActivityForResult(intent, SiteController.CONTROLLER_REQUEST_CODE);
		// FIXME not a safe cast, context might be a service
	}

	@Override
	public boolean upload(Account account, HashMap<String, String> valueMap, boolean useTor) {
		Log.d(TAG, "Upload file: Entering upload");
        
		String mediaUri = valueMap.get(VALUE_KEY_MEDIA_PATH);
		String mimeType = valueMap.get(VALUE_KEY_MIME_TYPE);

        String licenseUrl = valueMap.get(VALUE_KEY_LICENSE_URL);
        
		// TODO this should make sure we arn't accidentally using one of archive.org's metadata fields by accident
        String title = valueMap.get(VALUE_KEY_TITLE);
        String slug = valueMap.get(VALUE_KEY_SLUG);
		String tags = valueMap.get(VALUE_KEY_TAGS);
		//always want to include these two tags
		//tags += "presssecure,storymaker";
		String author = valueMap.get(VALUE_KEY_AUTHOR);
		String profileUrl = valueMap.get(VALUE_KEY_PROFILE_URL);
		String locationName = valueMap.get(VALUE_KEY_LOCATION_NAME);
		String body = valueMap.get(VALUE_KEY_BODY);


		OkHttpClient client = new OkHttpClient();

		if (useTor) {
			Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(ORBOT_HOST, ORBOT_HTTP_PORT));
			client.setProxy(proxy);
		}

        // FIXME we are putting a random 4 char string in the bucket name for collision avoidance, we might want to do this differently?
		String urlPath;
		String url;

        String randomString = new Util.RandomString(4).nextString();
        urlPath = slug + "-" + randomString;

		try {
			url = ARCHIVE_API_ENDPOINT  + "/" + urlPath + "/" + URLEncoder.encode(title,"UTF-8");
		} catch (UnsupportedEncodingException e) {
			Log.e(TAG, "Couldn't encode title",e);
			return false;
		}

		Log.d(TAG, "uploading to url: " + url);

		InputStream is = null;

		try {
			is = mContext.getContentResolver().openInputStream(Uri.parse(mediaUri));
		} catch (FileNotFoundException e) {
			Log.e(TAG, "Couldn't open stream",e);
			return false;
		}

		Request.Builder builder = new Request.Builder()
				.url(url)
				.put(RequestBodyUtil.create(MEDIA_TYPE, is))
				.addHeader("Accept", "*/*")
                .addHeader("x-amz-auto-make-bucket", "1")
//                .addHeader("x-archive-meta-collection", "storymaker")
//				.addHeader("x-archive-meta-sponsor", "Sponsor 998")
				.addHeader("x-archive-meta-language", "eng") // FIXME pull meta language from story
				.addHeader("authorization", "LOW " + account.getUserName() + ":" + account.getCredentials());

		if(!TextUtils.isEmpty(author)) {
			builder.addHeader("x-archive-meta-author", author);		
			if (profileUrl != null) {
				builder.addHeader("x-archive-meta-authorurl", profileUrl);
			}
        }

        if (mimeType != null) {
            builder.addHeader("x-archive-meta-mediatype", mimeType);
            if(mimeType.contains("audio")) {
                builder.addHeader("x-archive-meta-collection", "opensource_audio");
            } else {
                builder.addHeader("x-archive-meta-collection", "opensource_movies");
            }
        } else {
            builder.addHeader("x-archive-meta-collection", "opensource_movies");
		}

		if (!TextUtils.isEmpty(locationName)) {
			builder.addHeader("x-archive-meta-location", locationName);
		}

		if (!TextUtils.isEmpty(tags)) {
            String keywords = tags.replace(',', ';').replaceAll(" ", "");
            builder.addHeader("x-archive-meta-subject", keywords);
        }

		if (!TextUtils.isEmpty(body)) {
            builder.addHeader("x-archive-meta-description", body);
        }

		if (!TextUtils.isEmpty(title)) {
            builder.addHeader("x-archive-meta-title", title);
        }

		if (!TextUtils.isEmpty(licenseUrl)) {
            builder.addHeader("x-archive-meta-licenseurl", licenseUrl);
        }

		/*
		For uploads which need to be available ASAP in the content
  management system, an interactive user's upload for example,
  one can request interactive queue priority:
		 */
		builder.addHeader("x-archive-interactive-priority","1");
		
		Request request = builder.build();

		UploadFileTask uploadFileTask = new UploadFileTask(client, request);
		uploadFileTask.execute();

		return true;
	}

	class UploadFileTask extends AsyncTask<String, String, String> {
		private OkHttpClient client;
		private Request request;
		private Response response;

		public UploadFileTask(OkHttpClient client, Request request) {
			this.client = client;
			this.request = request;
		}

		@Override
		protected String doInBackground(String... params) {
			Log.d(TAG, "Begin Upload");

			try {
				response = client.newCall(request).execute();
                Log.d(TAG, "response: " + response + ", body: " + response.body().string());
				if (!response.isSuccessful()) {
					jobFailed(null, 4000001, "Archive upload failed: Unexpected Response Code: " + "response: " + response.code() + ": message=" + response.message());
				} else {	
				    jobSucceeded(response.request().urlString());
				}
			} catch (IOException e) {
				jobFailed(e, 4000002, "Archive upload failed: IOException");
				if (response != null && response.body() != null) {
					try {
						Log.d(TAG, response.body().string());
					} catch (IOException e1) {
						Log.d(TAG, "exception: " + e1.getLocalizedMessage() + ", stacktrace: " + e1.getStackTrace());
					}
				}
				else
				{



				}
			}

			return "-1";
		}
	}

    @Override
    public void startMetadataActivity(Intent intent) {
//        get the intent extras and launch the new intent with them
    }
}
