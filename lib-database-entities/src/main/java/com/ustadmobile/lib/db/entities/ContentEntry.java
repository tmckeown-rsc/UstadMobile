package com.ustadmobile.lib.db.entities;

import com.ustadmobile.lib.database.annotation.UmEntity;
import com.ustadmobile.lib.database.annotation.UmPrimaryKey;
import com.ustadmobile.lib.database.annotation.UmSyncLocalChangeSeqNum;
import com.ustadmobile.lib.database.annotation.UmSyncMasterChangeSeqNum;

import static com.ustadmobile.lib.db.entities.ContentEntry.TABLE_ID;

/**
 * Entity that represents content as it is browsed by the user. A ContentEntry can be either:
 *  1. An actual piece of content (e.g. book, course, etc), in which case there should be an associated
 *     ContentEntryFile.
 *  2. A navigation directory (e.g. a category as it is scraped from another site, etc), in which case
 *     there should be the appropriate ContentEntryParentChildJoin entities present.
 */
@UmEntity(tableId = TABLE_ID)
public class ContentEntry {

    public static final int TABLE_ID = 42;

    public static final int LICENSE_TYPE_CC_BY = 1;

    public static final int LICENSE_TYPE_CC_BY_SA = 2;

    public static final int LICENSE_TYPE_CC_BY_SA_NC = 3;

    public static final int LICENSE_TYPE_CC_BY_NC = 4;

    public static final int ALL_RIGHTS_RESERVED = 5;

    public static final int LICESNE_TYPE_CC_BY_NC_SA = 6;

    @UmPrimaryKey(autoGenerateSyncable = true)
    private long contentEntryUid;

    private String title;

    private String description;

    private String entryId;

    private String author;

    private String publisher;

    private int licenseType;

    private String licenseName;

    private String licenseUrl;

    private String sourceUrl;

    private String thumbnailUrl;

    private long lastModified;

    private long primaryLanguageUid;

    private long languageVariantUid;

    private boolean leaf;

    public long getLanguageVariantUid() {
        return languageVariantUid;
    }

    public void setLanguageVariantUid(long languageVariantUid) {
        this.languageVariantUid = languageVariantUid;
    }

    public long getPrimaryLanguageUid() {
        return primaryLanguageUid;
    }

    public void setPrimaryLanguageUid(long primaryLanguageUid) {
        this.primaryLanguageUid = primaryLanguageUid;
    }

    @UmSyncLocalChangeSeqNum
    private long contentEntryLocalChangeSeqNum;

    @UmSyncMasterChangeSeqNum
    private long contentEntryMasterChangeSeqNum;

    public long getContentEntryUid() {
        return contentEntryUid;
    }

    public void setContentEntryUid(long contentEntryUid) {
        this.contentEntryUid = contentEntryUid;
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

    /**
     * Get the embedded unique ID which can be found in the underlying file, if any. For
     * example the EPUB identifier for EPUB files, or the ID attribute of an xAPI zip file.
     *
     * @return The embedded unique ID which can be found in the underlying file
     */
    public String getEntryId() {
        return entryId;
    }

    /**
     * Set the embedded unique ID which can be found in the underlying file, if any. For
     * example the EPUB identifier for EPUB files, or the ID attribute of an xAPI zip file.
     *
     * @param entryId The embedded unique ID which can be found in the underlying file
     */
    public void setEntryId(String entryId) {
        this.entryId = entryId;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public int getLicenseType() {
        return licenseType;
    }

    public void setLicenseType(int licenseType) {
        this.licenseType = licenseType;
    }

    public String getLicenseName() {
        return licenseName;
    }

    public void setLicenseName(String licenseName) {
        this.licenseName = licenseName;
    }

    public String getLicenseUrl() {
        return licenseUrl;
    }

    public void setLicenseUrl(String licenseUrl) {
        this.licenseUrl = licenseUrl;
    }

    /**
     * Get the original URL this resource came from. In the case of resources that
     * were generated by scraping, this refers to the URL that the scraper targeted to
     * generated the resource.
     *
     * @return the original URL this resource came from
     */
    public String getSourceUrl() {
        return sourceUrl;
    }

    /**
     * Set the original URL this resource came from. In the case of resources that
     * were generated by scraping, this refers to the URL that the scraper targeted to
     * generated the resource.
     *
     * @param sourceUrl the original URL this resource came from
     */
    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }


    public String getThumbnailUrl() { return thumbnailUrl; }

    public void setThumbnailUrl(String thumbnailUrl){ this.thumbnailUrl = thumbnailUrl; }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }


    public long getContentEntryLocalChangeSeqNum() {
        return contentEntryLocalChangeSeqNum;
    }

    public void setContentEntryLocalChangeSeqNum(long contentEntryLocalChangeSeqNum) {
        this.contentEntryLocalChangeSeqNum = contentEntryLocalChangeSeqNum;
    }

    public long getContentEntryMasterChangeSeqNum() {
        return contentEntryMasterChangeSeqNum;
    }

    public void setContentEntryMasterChangeSeqNum(long contentEntryMasterChangeSeqNum) {
        this.contentEntryMasterChangeSeqNum = contentEntryMasterChangeSeqNum;
    }

    public boolean isLeaf() {
        return leaf;
    }

    public void setLeaf(boolean leaf) {
        this.leaf = leaf;
    }
}
