package com.ustadmobile.core.view;

import com.ustadmobile.core.db.UmProvider;
import com.ustadmobile.lib.db.entities.SelQuestion;
import com.ustadmobile.lib.db.entities.SelQuestionOption;

public interface SELQuestionDetail2View extends UstadView {
    String VIEW_NAME="SELQuestionDetail2";
    String ARG_QUESTION_UID_QUESTION_DETAIL = "ARGQuestionUidForQuestionDetail";
    String ARG_QUESTION_OPTION_UID = "ArgQuestionOptionUid";
    void setQuestionOptionsProvider(UmProvider<SelQuestionOption> listProvider);
    void setQuestionTypePresets(String[] presets);
    void finish();
    void setQuestionText(String questionText);
    void setQuestionType(int type);
    void handleClickDone();
    void showQuestionOptions(boolean show);
    void handleQuestionTypeChange(int type);
    void handleClickAddOption();
    void setQuestionTypeListener();
    void setQuestionOnView(SelQuestion selQuestion);
}