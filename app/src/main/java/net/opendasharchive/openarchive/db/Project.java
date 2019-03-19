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

    @Ignore
    private ArrayList<Media> mediaArrayList;

    public static List<Project> getAllAsList() {
        return Project.find(Project.class,null,null,null,"ID DESC",null);
    }

    public static Project getById(long projectId) {
        return Project.findById(Project.class, projectId);
    }

    public static boolean deleteById(long projectId) {
        Project project = Project.findById(Project.class, projectId);
        return project.delete();
    }
}
