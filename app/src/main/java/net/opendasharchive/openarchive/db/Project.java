package net.opendasharchive.openarchive.db;

import com.orm.SugarRecord;
import com.orm.dsl.Ignore;

import java.util.ArrayList;
import java.util.Date;

public class Project extends SugarRecord {

    public String description;
    public Date created;

    @Ignore
    private ArrayList<Media> mediaArrayList;
}
