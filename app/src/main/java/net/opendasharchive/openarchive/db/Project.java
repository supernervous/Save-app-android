package net.opendasharchive.openarchive.db;

import com.orm.SugarRecord;
import com.orm.dsl.Ignore;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Project extends SugarRecord {

    public String description;
    public Date created;

    public long spaceId;
    public boolean archived = false;

    public long openCollectionId;


    public String licenseUrl;


    @Ignore
    private ArrayList<Media> mediaArrayList;

    public static List<Project> getAllBySpace(long spaceId) {
        String[] whereArgs = {spaceId + ""};
        return Project.find(Project.class,"space_id = ?",whereArgs,null,"ID DESC",null);
    }


    public static List<Project> getAllBySpace(long spaceId, boolean archived) {

        int isArchived = archived?1:0;
        String[] whereArgs = {spaceId + "", isArchived+""};

        return Project.find(Project.class,"space_id = ? AND archived = ?",whereArgs,null,"ID DESC",null);
    }

    public static Project getById(long projectId) {
        return Project.findById(Project.class, projectId);
    }

    public static boolean deleteById(long projectId) {
        Project project = Project.findById(Project.class, projectId);
        return project.delete();
    }


    public long getSpaceId() {
        return spaceId;
    }

    public void setSpaceId(long spaceId) {
        this.spaceId = spaceId;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public long getOpenCollectionId() {
        return openCollectionId;
    }

    public void setOpenCollectionId(long openCollectionId) {
        this.openCollectionId = openCollectionId;
    }


    public String getLicenseUrl() {
        return licenseUrl;
    }

    public void setLicenseUrl(String newLicenseUrl) {
        this.licenseUrl = newLicenseUrl;
    }

}
