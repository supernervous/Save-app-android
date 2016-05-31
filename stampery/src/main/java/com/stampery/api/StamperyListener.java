package com.stampery.api;

/**
 * Created by n8fr8 on 5/30/16.
 */
public interface StamperyListener {

    public void stampSuccess (String action, String hash);

    public void stampFailed (String action, Exception e);
}
