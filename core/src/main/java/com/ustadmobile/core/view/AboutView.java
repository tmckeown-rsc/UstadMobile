package com.ustadmobile.core.view;

/**
 * Created by mike on 12/27/16.
 */

public interface AboutView extends UstadView {

    String VIEW_NAME = "About";

    /**
     * Sets version of the application in the about screen.
     * @param versionInfo   The version info string. Eg: v0.98.2 (#190) - Sun, 05 May 2019 09:18:45 GMT
     */
    void setVersionInfo(String versionInfo);

    /**
     * Sets html plan text string given to it to into an html view component on the view.
     * @param aboutHTML The html plain text string.
     */
    void setAboutHTML(String aboutHTML);
}
