package net.opendasharchive.openarchive.services;

import android.content.Context;
import android.content.Intent;

import net.opendasharchive.openarchive.db.Media;
import net.opendasharchive.openarchive.db.Space;

import java.util.HashMap;

import io.scal.secureshareui.controller.SiteController;
import io.scal.secureshareui.controller.SiteControllerListener;

public class SCPSiteController extends SiteController {

    public SCPSiteController(Context context, SiteControllerListener listener, String jobId) {
        super(context, listener, jobId);
    }

    @Override
    public void startRegistration(Space space) {

    }

    @Override
    public void startAuthentication(Space space) {

    }

    @Override
    public void startMetadataActivity(Intent intent) {

    }

    @Override
    public boolean upload(Space space, Media media, HashMap<String, String> valueMap) {
        return false;
    }

    @Override
    public boolean delete(Space space, String bucketName, String mediaFile) {
        return false;
    }
}
