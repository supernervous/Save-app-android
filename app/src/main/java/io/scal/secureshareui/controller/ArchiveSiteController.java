package io.scal.secureshareui.controller;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;

import net.gotev.uploadservice.BinaryUploadRequest;
import net.gotev.uploadservice.ServerResponse;
import net.gotev.uploadservice.UploadInfo;
import net.gotev.uploadservice.UploadNotificationConfig;
import net.gotev.uploadservice.UploadStatusDelegate;
import net.opendasharchive.openarchive.db.Media;
import net.opendasharchive.openarchive.util.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import io.scal.secureshareui.lib.Util;
import io.scal.secureshareui.login.ArchiveLoginActivity;
import io.scal.secureshareui.model.Account;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ArchiveSiteController extends SiteController {

	public static final String SITE_NAME = "Internet Archive";
	public static final String SITE_KEY = "archive";
	private static final String TAG = "ArchiveSiteController";
    static {
        METADATA_REQUEST_CODE = 1022783271;
    }

	private static final String ARCHIVE_API_ENDPOINT = "https://s3.us.archive.org";


    private final static String ARCHIVE_DETAILS_ENDPOINT = "https://archive.org/details/";

	public static final MediaType MEDIA_TYPE = MediaType.parse("");

	public ArchiveSiteController(Context context, SiteControllerListener listener, String jobId) {
		super(context, listener, jobId);
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


    public String uploadNew(final Media media, Account account, HashMap<String, String> valueMap) {
        try {
            // starting from 3.1+, you can also use content:// URI string instead of absolute file
            String mediaUri = valueMap.get(VALUE_KEY_MEDIA_PATH);
            String mimeType = valueMap.get(VALUE_KEY_MIME_TYPE);
            String licenseUrl = valueMap.get(VALUE_KEY_LICENSE_URL);

            // TODO this should make sure we arn't accidentally using one of archive.org's metadata fields by accident
            String title = valueMap.get(VALUE_KEY_TITLE);
            String slug = valueMap.get(VALUE_KEY_SLUG);
            String tags = valueMap.get(VALUE_KEY_TAGS);
            String author = valueMap.get(VALUE_KEY_AUTHOR);
            String profileUrl = valueMap.get(VALUE_KEY_PROFILE_URL);
            String locationName = valueMap.get(VALUE_KEY_LOCATION_NAME);
            String body = valueMap.get(VALUE_KEY_BODY);

            String uploadFilePath = mediaUri;
            if (uploadFilePath.startsWith("file:"))
                uploadFilePath = new File(Uri.parse(mediaUri).getPath()).getCanonicalPath();

            String uploadBasePath;
            String uploadPath;

            String randomString = new Util.RandomString(4).nextString();
            uploadBasePath = slug + "-" + randomString;

             uploadPath = "/" + uploadBasePath + "/" + getTitleFileName(media);

            media.setServerUrl(ARCHIVE_DETAILS_ENDPOINT + uploadBasePath);

            UploadNotificationConfig notConfig = new UploadNotificationConfig();
            notConfig.setTitleForAllStatuses(title);

            BinaryUploadRequest builder = new BinaryUploadRequest(mContext, ARCHIVE_API_ENDPOINT + uploadPath)
                    .setMethod("PUT")
                            .setFileToUpload(uploadFilePath)
                            .setNotificationConfig(notConfig)
                            .setMaxRetries(3)
                    .addHeader("Accept", "*/*")
                    .addHeader("x-archive-auto-make-bucket","1")
                    .addHeader("x-amz-auto-make-bucket", "1")
//                .addHeader("x-archive-meta-collection", "storymaker")
//				.addHeader("x-archive-meta-sponsor", "Sponsor 998")
                    .addHeader("x-archive-meta-language", "eng") // FIXME set based on locale or selected
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
                } else if (mimeType.contains("image")) {
                    builder.addHeader("x-archive-meta-collection", "opensource_media");
                }
                else {
                    builder.addHeader("x-archive-meta-collection", "opensource_movies");
                }
            } else {
                builder.addHeader("x-archive-meta-collection", "opensource_media");
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

            builder.setDelegate(new UploadStatusDelegate() {
                                      @Override
                                      public void onProgress(Context context, UploadInfo uploadInfo) {
                                          // your code here
                                          jobProgress((((float)uploadInfo.getProgressPercent())/100f),uploadInfo.toString());
                                      }

                                      @Override
                                      public void onError(Context context, UploadInfo uploadInfo, ServerResponse serverResponse,
                                                          Exception exception) {
                                          String err = uploadInfo.getUploadId();
                                          if (serverResponse != null)
                                              err = serverResponse.getBodyAsString();

                                          jobFailed(exception,-1,err);
                                      }

                                      @Override
                                      public void onCompleted(Context context, UploadInfo uploadInfo, ServerResponse serverResponse) {
                                          // your code here
                                          // if you have mapped your server response to a POJO, you can easily get it:
                                          // YourClass obj = new Gson().fromJson(serverResponse.getBodyAsString(), YourClass.class);
                                          jobSucceeded(serverResponse.getBodyAsString());

                                      }

                                      @Override
                                      public void onCancelled(Context context, UploadInfo uploadInfo) {
                                          // your code here
                                          jobFailed(new Exception("Cancelled"),-1,uploadInfo.toString());
                                      }
                                  });

            String uploadId = builder.startUpload();

            return uploadId;

        } catch (Exception exc) {
            Log.e("AndroidUploadService", exc.getMessage(), exc);
        }

        return null;
    }

    private String getArchiveUploadEndpoint (String title, String slug, String mimeType)
    {
        String urlPath;
        String url;
        String ext;

        String randomString = new Util.RandomString(4).nextString();
        urlPath = slug + "-" + randomString;
        ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);

        if (TextUtils.isEmpty(ext))
        {
            if (mimeType.startsWith("image"))
                ext = "jpg";
            else if (mimeType.startsWith("video"))
                ext = "mp4";
            else if (mimeType.startsWith("video"))
                ext = "m4a";
            else
                ext = "txt";

        }

        try {
            url = "/" + urlPath + "/" + URLEncoder.encode(title,"UTF-8") + '.' + ext;


        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Couldn't encode title",e);
            return null;
        }

        return url;

    }

    public static String getTitleFileName (Media media) {
        String filename = null;

        String ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(media.getMimeType());

        if (TextUtils.isEmpty(ext))
        {
            if (media.getMimeType().startsWith("image"))
                ext = "jpg";
            else if (media.getMimeType().startsWith("video"))
                ext = "mp4";
            else if (media.getMimeType().startsWith("audio"))
                ext = "m4a";
            else
                ext = "txt";

        }

        try {
            filename = URLEncoder.encode(media.getTitle(),"UTF-8") + '.' + ext;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return filename;
    }

    @Override
    public boolean delete(Account account, String title, String mediaFile) {
        Log.d(TAG, "Upload file: Entering upload");

        /**
         *
         o DELETE normally deletes a single file, additionally all the
         derivatives and originals related to a file can be
         automatically deleted by specifying a header with the DELETE
         like so:
         x-archive-cascade-delete:1
         */

        OkHttpClient client = new OkHttpClient();

        // FIXME we are putting a random 4 char string in the bucket name for collision avoidance, we might want to do this differently?

        String mediaUrl = null;

        try {
            mediaUrl = ARCHIVE_API_ENDPOINT + '/' + URLEncoder.encode(title,"UTF-8") + '/' + mediaFile;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return false;
        }

        Log.d(TAG, "deleting url media item: " + mediaUrl);


        Request.Builder builder = new Request.Builder()
                .delete()
                .url(mediaUrl)
                .addHeader("Accept", "*/*")
                .addHeader("x-archive-cascade-delete", "1")
                .addHeader("authorization", "LOW " + account.getUserName() + ":" + account.getCredentials());

        Request request = builder.build();

        ArchiveServerTask deleteFileTask = new ArchiveServerTask(client, request);
        deleteFileTask.execute();

        return true;
    }

	@Override
	public boolean upload(Account account, HashMap<String, String> valueMap) {
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
				//.put(RequestBodyUtil.create(MediaType.parse(""),is))
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
            } else  if(mimeType.contains("video")) {
                builder.addHeader("x-archive-meta-collection", "opensource_movies");
            }
            else
            {
                builder.addHeader("x-archive-meta-collection", "opensource");
            }
        } else {
            builder.addHeader("x-archive-meta-collection", "opensource");
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

        ArchiveServerTask uploadFileTask = new ArchiveServerTask(client, request);
		uploadFileTask.execute();

		return true;
	}

	class ArchiveServerTask extends AsyncTask<String, String, String> {
		private OkHttpClient client;
		private Request request;
		private Response response;

		public ArchiveServerTask(OkHttpClient client, Request request) {
			this.client = client;
			this.request = request;
		}

		@Override
		protected String doInBackground(String... params) {
			Log.d(TAG, "Begin Upload");

			try {
				/**
			    int timeout = 60 * 1000 * 2; //2 minute timeout!

				client.setConnectTimeout(timeout, TimeUnit.MILLISECONDS);
				client.setWriteTimeout(timeout, TimeUnit.MILLISECONDS);
                client.setReadTimeout(timeout, TimeUnit.MILLISECONDS);

				 	**/

				response = client.newCall(request).execute();
                Log.d(TAG, "response: " + response + ", body: " + response.body().string());
				if (!response.isSuccessful()) {
					jobFailed(null, 4000001, "Archive upload failed: Unexpected Response Code: " + "response: " + response.code() + ": message=" + response.message());
				} else {	
				    jobSucceeded(response.request().toString());
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
