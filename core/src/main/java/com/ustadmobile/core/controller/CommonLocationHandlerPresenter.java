package com.ustadmobile.core.controller;

import com.ustadmobile.core.view.UstadView;

import java.util.Hashtable;

/**
 * So that we can add click listener for different use cases.
 * @param <V>   The view
 */
public abstract class CommonLocationHandlerPresenter<V extends UstadView>
        extends UstadBaseController<V>  {

    public CommonLocationHandlerPresenter(Object context) {
        super(context);
    }

    //The constructor will throw an uncast check warning. That is expected.
    public CommonLocationHandlerPresenter(Object context, Hashtable arguments, UstadView view) {
        super(context, arguments, (V) view);
    }

    public abstract void locationChecked(String locationName, Long locationUid, boolean checked);
}