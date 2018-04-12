package io.scal.secureshareui.controller;

import android.os.Message;

public interface SiteControllerListener {

    public void success (Message msg);

    public void progress (Message msg);

    public void failure (Message msg);
}
