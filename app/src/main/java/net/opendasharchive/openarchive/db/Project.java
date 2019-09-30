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

    @Ignore
    private ArrayList<Media> mediaArrayList;

    public static List<Project> getAllAsList() {
        return Project.find(Project.class,null,null,null,"ID DESC",null);
    }


    public static List<Project> getAllAsList(boolean archived) {

        int isArchived = archived?1:0;
        String[] whereArgs = {isArchived+""};

        return Project.find(Project.class,"archived = ?",whereArgs,null,"ID DESC",null);
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

}
