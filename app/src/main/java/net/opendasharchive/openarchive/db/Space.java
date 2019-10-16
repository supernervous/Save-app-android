package net.opendasharchive.openarchive.db;

import com.orm.SugarRecord;

import net.opendasharchive.openarchive.util.Prefs;

import java.util.Iterator;
import java.util.List;

public class Space extends SugarRecord {

    public int type;
    public String name;

    public String username;
    public String password;

    public String host;

    public final static int TYPE_WEBDAV = 0;
    public final static int TYPE_INTERNET_ARCHIVE = 1;
    public final static int TYPE_PIRATEBOX = 2;
    public final static int TYPE_DROPBOX = 3;
    public final static int TYPE_DAT = 4;
    public final static int TYPE_SCP = 5;

    public static Iterator<Space> getAllAsList() {
        return Space.findAll(Space.class);
    }

    public static Space getCurrentSpace ()
    {
        long spaceId = Prefs.getCurrentSpaceId();
        if (spaceId != -1L) {
            return Space.findById(Space.class,spaceId);
        }

        return null;
    }
}
