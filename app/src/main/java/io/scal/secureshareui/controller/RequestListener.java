package io.scal.secureshareui.controller;

public interface RequestListener {

    public void transferred(long bytes);

    public boolean continueUpload();

    public void transferComplete ();
}
