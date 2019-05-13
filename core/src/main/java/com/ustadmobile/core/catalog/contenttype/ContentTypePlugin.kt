package com.ustadmobile.core.catalog.contenttype

/**
 * A ContentTypePlugin provides support to use a specific file type (e.g. EPUB, Xapi Package, etc) on
 * Ustad Mobile. Specifically a plugin is responsible to:
 *
 * a. Read a file type and return information about it including a unique id and a title.
 * b. Provide the view name that will be used to view that item.
 *
 * Created by mike on 9/9/17.
 */
abstract class ContentTypePlugin {

    //    /**
    //     *
    //     */
    //    public interface EntryResult {
    //
    //        UstadJSOPDSFeed getFeed();
    //
    //        InputStream getThumbnail() throws IOException;
    //
    //        String getThumbnailMimeType();
    //    }

    /**
     * Return a String that will match the VIEW_NAME for the view that should be opened for this
     * type of content
     *
     * @return Name of view to open for this type of content
     */
    abstract val viewName: String

    /**
     * Return an array of mime types that are used by this content type.
     *
     * @return
     */
    abstract val mimeTypes: List<String>

    abstract val fileExtensions: List<String>


}