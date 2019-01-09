package net.opendasharchive.openarchive.db;

import com.orm.SugarRecord;

public class Space extends SugarRecord {

    public int type;
    public String name;

    public String username;
    public String password;

    public String host;

    public final static int TYPE_PRIVATE = 0;
    public final static int TYPE_INTERNET_ARCHIVE = 1;
    public final static int TYPE_NEARBY = 2;

}
