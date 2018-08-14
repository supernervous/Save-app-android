package net.opendasharchive.openarchive.services;

import android.content.Context;
import android.content.Intent;

import net.opendasharchive.openarchive.db.Media;

import java.util.HashMap;

import io.scal.secureshareui.controller.SiteController;
import io.scal.secureshareui.controller.SiteControllerListener;
import io.scal.secureshareui.model.Account;

public class SCPSiteController extends SiteController {

    public SCPSiteController(Context context, SiteControllerListener listener, String jobId) {
        super(context, listener, jobId);
    }

    @Override
    public void startRegistration(Account account) {

    }

    @Override
    public void startAuthentication(Account account) {

    }

    @Override
    public void startMetadataActivity(Intent intent) {

    }

    @Override
    public boolean upload(Account account, Media media, HashMap<String, String> valueMap) {
        return false;
    }

    @Override
    public boolean delete(Account account, String bucketName, String mediaFile) {
        return false;
    }
}
