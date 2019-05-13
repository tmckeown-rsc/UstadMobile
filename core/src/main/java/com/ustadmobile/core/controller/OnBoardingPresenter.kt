package com.ustadmobile.core.controller

import com.ustadmobile.core.impl.UstadMobileSystemImpl
import com.ustadmobile.core.view.DummyView
import com.ustadmobile.core.view.OnBoardingView
import com.ustadmobile.core.view.OnBoardingView.Companion.PREF_TAG

class OnBoardingPresenter(context: Any, arguments: Map<String, String>?, view: OnBoardingView) : UstadBaseController<OnBoardingView>(context, arguments!!, view) {

    val impl : UstadMobileSystemImpl = UstadMobileSystemImpl.instance
    override fun onCreate(savedState: Map<String, String?> ?) {
        super.onCreate(savedState)
        view.runOnUiThread(Runnable  { view.setScreenList() })

        val wasShown = impl.getAppPref(PREF_TAG, view.context)
        if (wasShown!= null && wasShown.toBoolean()) {
            handleGetStarted()
        }
    }


    fun handleGetStarted() {
        val args: Map<String,String?> = arguments;
        impl.setAppPref(PREF_TAG, true.toString(), view.context)
        impl.go(DummyView.VIEW_NAME, args, context)
    }
}