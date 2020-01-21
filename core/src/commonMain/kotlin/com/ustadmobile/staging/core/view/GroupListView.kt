package com.ustadmobile.core.view

import androidx.paging.DataSource
import com.ustadmobile.lib.db.entities.GroupWithMemberCount


/**
 * Core View. Screen is for GroupList's View
 */
interface GroupListView : UstadView {

    //Any argument keys:

    /**
     * Method to finish the screen / view.
     */
    fun finish()


    /**
     * Sets the given provider to the view's provider adapter.
     *
     * @param listProvider The provider to set to the view
     */
    fun setListProvider(listProvider: DataSource.Factory<Int, GroupWithMemberCount>)

    companion object {


        // This defines the view name that is an argument value in the go() in impl.
        val VIEW_NAME = "GroupList"
    }


}
