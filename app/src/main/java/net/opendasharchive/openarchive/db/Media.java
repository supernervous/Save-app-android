package net.opendasharchive.openarchive.db;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import com.orm.SugarRecord;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import net.opendasharchive.openarchive.R;
import net.opendasharchive.openarchive.util.Utility;

/**
 * Created by micahjlucas on 1/11/15.
 */
public class Media extends SugarRecord {

    public String originalFilePath;
    public String mimeType;
    public Date createDate;
    public Date updateDate;
    public String serverUrl;

    public String title;
    public String description;
    public String author;
    public String location;
    public String tags;

    public String licenseUrl;
    public byte[] mediaHash;

    public int status;

    public long projectId;

    public final static int STATUS_ERROR = 9;
    public final static int STATUS_DELETE_LOCAL = 6;
    public final static int STATUS_DELETE_REMOTE = 7;
    public final static int STATUS_ARCHIVED = 5;
    public final static int STATUS_PUBLISHED = 3;
    public final static int STATUS_UPLOADING = 4;
    public final static int STATUS_QUEUED = 2;
    public final static int STATUS_LOCAL = 1;
    public final static int STATUS_NEW = 0;

    private final static String[] WHERE_NOT_DELETED = {STATUS_PUBLISHED+""};

    public static enum MEDIA_TYPE {
        AUDIO, IMAGE, VIDEO, FILE;
    }

    //left public ONLY for Sugar ORM
    public Media() {};

    /* getters and setters */
    public String getOriginalFilePath() {
        return this.originalFilePath;
    }
    public void setOriginalFilePath(String originalFilePath) {
        this.originalFilePath = originalFilePath;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public Date getCreateDate() {
        return this.createDate;
    }
    public String getFormattedCreateDate() {
        if (this.createDate != null)
            return SimpleDateFormat.getDateInstance(SimpleDateFormat.SHORT).format(this.createDate);
        else
            return "";
    }
    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public Date getUpdateDate() {
        return this.updateDate;
    }
    public void setUpdateDate(Date updateDate) {
        this.updateDate = updateDate;
    }

    public String getServerUrl() {
        return this.serverUrl;
    }
    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getTags() {
        return tags;
    }

    public String getLicenseUrl() {
        return licenseUrl;
    }

    public void setLicenseUrl(String licenseUrl) {
        this.licenseUrl = licenseUrl;
    }

    public byte[] getMediaHash ()
    {
        return mediaHash;
    }

    public void setMediaHash (byte[] mediaHash)
    {
        this.mediaHash = mediaHash;
    }

    public void setTags(String tags) {
        // repace spaces and commas with semicolons
        tags = tags.replace(' ', ';');
        tags = tags.replace(',', ';');
        this.tags = tags;
    }

    public static List<Media> getAllMediaAsList() {
        return Media.find(Media.class,"status <= ?",WHERE_NOT_DELETED,null,"ID DESC",null);
       // return Media.listAll(Media.class,);
    }

    public static List<Media> getMediaByProject(long projectId) {
        String[] values = {projectId+""};
        return Media.find(Media.class,"PROJECT_ID = ?",values,null,"ID DESC",null);
    }


    public static Media getMediaById(long mediaId) {
        return Media.findById(Media.class, mediaId);
    }

    public static boolean deleteMediaById(long mediaId) {
        Media media = Media.findById(Media.class, mediaId);
        return media.delete();
    }

    public void setProjectId (long projectId)
    {
        this.projectId = projectId;
    }

    public long getProjectId ()
    {
        return this.projectId;
    }
}

/**
 archive.org JSON for the media file

 {
 "server":"ia801509.us.archive.org",
 "dir":"/8/items/peter-at-the-park-zaav",
 "metadata":{"identifier":["peter-at-the-park-zaav"],"collection":["opensource_movies"],
 "language":["eng"],"licenseurl":["https:\/\/creativecommons.org\/licenses\/by\/4.0\/"],
 "mediatype":["movies"],"title":["peter at the park"],"publicdate":["2016-05-16 19:00:00"],"addeddate":["2016-05-16 19:00:00"],
 "curation":["[curator]validator@archive.org[\/curator][date]20160516190255[\/date][comment]checked for malware[\/comment]"]},
 "reviews":{"info":{"num_reviews":1,"avg_rating":"5.00"},"reviews":[{"reviewbody":"Looks like a grand old time!","reviewtitle":"What fun!","reviewer":"n8fr8","reviewdate":"2016-05-16 19:40:17","stars":"5"}]},
 "files":
 {"\/VID_20160514_104759.mp4":{"source":"original","mtime":"1463425199","size":"44370800","md5":"d5592835a5fb0ea63f4fe8ca37f15c33","crc32":"769ba0da","sha1":"d20e4261696e045e8996a9e500dcd658c095c69b","format":"MPEG4","length":"35.23","height":"720","width":"1280"},"\/VID_20160514_104759.ogv":{"source":"derivative","format":"Ogg Video","original":"VID_20160514_104759.mp4","mtime":"1463425476","size":"2794303","md5":"9bfc10612019084dd1976fcd4b29f17b","crc32":"4dbbe6f2","sha1":"e0a5c70c513392564309c7a80936fa646d5a5efc","length":"35.23","height":"300","width":"533"},"\/peter-at-the-park-zaav.thumbs\/VID_20160514_104759_000001.jpg":{"source":"derivative","format":"Thumbnail","original":"VID_20160514_104759.mp4","mtime":"1463425391","size":"8236","md5":"ddcc6d5ef4023b55b6b806db70424086","crc32":"00c357f5","sha1":"262c1ab6907de0924ad9d93a5a2029b8844802d7"},"\/peter-at-the-park-zaav.thumbs\/VID_20160514_104759_000009.jpg":{"source":"derivative","format":"Thumbnail","original":"VID_20160514_104759.mp4","mtime":"1463425393","size":"9802","md5":"8f2550ed2f737c7ce261ac6d613bf0b6","crc32":"428ff9ab","sha1":"4f4a842eedeb00fe0379ef33d2e4f28d0a837465"},"\/peter-at-the-park-zaav.thumbs\/VID_20160514_104759_000014.jpg":{"source":"derivative","format":"Thumbnail","original":"VID_20160514_104759.mp4","mtime":"1463425395","size":"6578","md5":"74d61aba313a59f968f443420eca09c4","crc32":"e6ee50cb","sha1":"eca6f51ce36d871c5d801893243f09c83d3f1da2"},"\/peter-at-the-park-zaav.thumbs\/VID_20160514_104759_000019.jpg":{"source":"derivative","format":"Thumbnail","original":"VID_20160514_104759.mp4","mtime":"1463425397","size":"5313","md5":"24bf7ec4699ab06b0b96f0dbcd226787","crc32":"5f290f25","sha1":"19a1b07aa1450fea2c72e00b203b34344f03573a"},"\/peter-at-the-park-zaav.thumbs\/VID_20160514_104759_000024.jpg":{"source":"derivative","format":"Thumbnail","original":"VID_20160514_104759.mp4","mtime":"1463425399","size":"2892","md5":"0ede33d520f1944553c8ec9025306946","crc32":"e957e3cf","sha1":"72ab1d32cb2a819419bf5785acce05a6f96cad03"},"\/peter-at-the-park-zaav.thumbs\/VID_20160514_104759_000029.jpg":{"source":"derivative","format":"Thumbnail","original":"VID_20160514_104759.mp4","mtime":"1463425401","size":"3033","md5":"7c3cbfe5fcb2849db95b851fe4be4ffc","crc32":"63eb8d36","sha1":"373c150c9c65c6c69be97625b180a15e43fb3057"},"\/peter-at-the-park-zaav.thumbs\/VID_20160514_104759_000034.jpg":{"source":"derivative","format":"Thumbnail","original":"VID_20160514_104759.mp4","mtime":"1463425403","size":"2580","md5":"dbee4ed4e980dcc7c49b51e081b94ff8","crc32":"51cdfd52","sha1":"0582973d49ab0b3f25aa0cb28f9e79ffc6367ae4"},"\/peter-at-the-park-zaav_archive.torrent":{"source":"original","btih":"1460c3a31604225a80d99a50f0796a919e829c24","mtime":"1463427620","size":"3707","md5":"c42f50b195c9ecfd98e523cf91237161","crc32":"c2afbd91","sha1":"348867111d827842accc4827a9cdcbbea8b43bd1","format":"Archive BitTorrent"},"\/peter-at-the-park-zaav_files.xml":{"source":"original","format":"Metadata","md5":"d44bc630f82e3bd74c6bb684e671a7ee"},"\/peter-at-the-park-zaav_meta.sqlite":{"source":"original","mtime":"1463425341","size":"6144","md5":"cdb6a4ebd08123e966223cfd99e189d5","crc32":"0cd3d018","sha1":"f513ea9fb90c70f6aaea9d62718c174b4144a732","format":"Metadata"},"\/peter-at-the-park-zaav_meta.xml":{"source":"original","mtime":"1463425375","size":"591","format":"Metadata","md5":"08dcf6878c72d1afa7d26156bf1c1e33","crc32":"51be65f9","sha1":"1f8ee82d7cd53b85bb16f221f40e8382ce5888e4"},"\/peter-at-the-park-zaav_reviews.xml":{"source":"original","mtime":"1463427619","size":"458","md5":"b2319b56f30374d09866903c1a59d42c","crc32":"6e44cf91","sha1":"0fb189dd0e87f12eaeff5658309f09634d4f869d","format":"Metadata"}},
 "misc":
 {"image":"\/\/archive.org\/services\/img\/opensource_movies&fallback=1","collection-title":"Community Video"}}

 **/