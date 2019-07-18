package net.opendasharchive.openarchive.db;

import com.orm.SugarRecord;

import java.util.Date;
import java.util.List;

public class Collection extends SugarRecord {


    public long projectId;

    public Date uploadDate;

    public String serverUrl;

    public long getProjectId() {
        return projectId;
    }

    public void setProjectId(long projectId) {
        this.projectId = projectId;
    }

    public Date getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(Date uploadDate) {
        this.uploadDate = uploadDate;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public static List<Collection> getAllAsList() {
        return Collection.find(Collection.class,null,null,null,"ID DESC",null);
    }

    public static List<Collection> getAllAsListByProject(long projectId) {
        String[] values = {projectId+""};
        return Collection.find(Collection.class,"PROJECT_ID = ?",values,null,"ID DESC",null);
    }
}
