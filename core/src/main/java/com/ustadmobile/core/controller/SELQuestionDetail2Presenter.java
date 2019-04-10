package com.ustadmobile.core.controller;

import com.ustadmobile.core.db.UmAppDatabase;
import com.ustadmobile.core.db.UmProvider;
import com.ustadmobile.core.db.dao.SelQuestionDao;
import com.ustadmobile.core.db.dao.SelQuestionOptionDao;
import com.ustadmobile.core.generated.locale.MessageID;
import com.ustadmobile.core.impl.UmAccountManager;
import com.ustadmobile.core.db.UmLiveData;
import com.ustadmobile.core.impl.UmCallback;
import com.ustadmobile.core.impl.UstadMobileSystemImpl;
import com.ustadmobile.core.view.AddQuestionOptionDialogView;
import com.ustadmobile.core.view.SELQuestionDetail2View;
import com.ustadmobile.lib.db.entities.SelQuestion;
import com.ustadmobile.lib.db.entities.SelQuestionOption;

import java.util.Hashtable;

import static com.ustadmobile.core.view.SELQuestionDetail2View.ARG_QUESTION_OPTION_UID;
import static com.ustadmobile.core.view.SELQuestionDetail2View.ARG_QUESTION_UID_QUESTION_DETAIL;
import static com.ustadmobile.core.view.SELQuestionSetDetailView.ARG_SEL_QUESTION_SET_UID;

public class SELQuestionDetail2Presenter extends
        UstadBaseController<SELQuestionDetail2View> {


    //Provider
    private UmProvider<SelQuestionOption> providerList;
    UmAppDatabase repository;
    private long currentQuestionUid;
    private long currentQuestionSetUid;
    UmLiveData<SelQuestion> questionUmLiveData;

    private SelQuestion mOriginalQuestion;
    private SelQuestion mUpdatedQuestion;
    SelQuestionDao questionDao;
    SelQuestionOptionDao questionOptionDao;

    private String[] questionTypePresets;

    public SELQuestionDetail2Presenter(Object context, Hashtable arguments,
                                       SELQuestionDetail2View view) {
        super(context, arguments, view);


        if (arguments.containsKey(ARG_SEL_QUESTION_SET_UID)) {
            currentQuestionSetUid = (long) arguments.get(ARG_SEL_QUESTION_SET_UID);

        }
        if(arguments.containsKey(ARG_QUESTION_UID_QUESTION_DETAIL)){
            currentQuestionUid = (long) arguments.get(ARG_QUESTION_UID_QUESTION_DETAIL);
        }
    }

    @Override
    public void onCreate(Hashtable savedState){
        super.onCreate(savedState);

        repository = UmAccountManager.getRepositoryForActiveAccount(context);
        questionDao = repository.getSocialNominationQuestionDao();
        questionOptionDao = repository.getSELQuestionOptionDao();

        providerList = repository.getSELQuestionOptionDao()
                .findAllActiveOptionsByQuestionUidProvider(currentQuestionUid);



        //Set questionType preset
        UstadMobileSystemImpl impl = UstadMobileSystemImpl.getInstance();
        questionTypePresets = new String[]{
                impl.getString(MessageID.sel_question_type_nomination, context),
                impl.getString(MessageID.sel_question_type_multiple_choise, context),
                impl.getString(MessageID.sel_question_type_free_text, context)};

        //Set to view
        view.setQuestionTypePresets(questionTypePresets);

        //Create / Get question
        questionUmLiveData =
                repository.getSocialNominationQuestionDao().findByUidLive(currentQuestionUid);

        //Observe the live data :
        questionUmLiveData.observe(SELQuestionDetail2Presenter.this,
                SELQuestionDetail2Presenter.this::handleSELQuestionValueChanged);

        SelQuestionDao selQuestionDao =
                repository.getSocialNominationQuestionDao();
        selQuestionDao.findByUidAsync(currentQuestionUid,
                new UmCallback<SelQuestion>() {
            @Override
            public void onSuccess(SelQuestion selQuestion) {
                if(selQuestion != null){
                    mUpdatedQuestion = selQuestion;
                    view.setQuestionOnView(mUpdatedQuestion);
                }else{

                    //Set index
                    selQuestionDao.getMaxIndexByQuestionSetAsync(currentQuestionSetUid,
                        new UmCallback<Integer>() {
                            @Override
                            public void onSuccess(Integer result) {
                                //Create a new one
                                SelQuestion newSELQuestion = new SelQuestion();
                                newSELQuestion.setSelQuestionSelQuestionSetUid(
                                        currentQuestionSetUid);
                                newSELQuestion.setQuestionIndex(result+1);
                                mUpdatedQuestion = newSELQuestion;

                                if(mOriginalQuestion == null){
                                    mOriginalQuestion = mUpdatedQuestion;
                                }

                                view.setQuestionOnView(mUpdatedQuestion);
                            }

                            @Override
                            public void onFailure(Throwable exception) {
                                exception.printStackTrace();
                            }
                        });
                }
            }

            @Override
            public void onFailure(Throwable exception) {
                exception.printStackTrace();
            }
        });

        //Set provider
        view.setQuestionOptionsProvider(providerList);

    }

    public void handleQuestionTypeChange(int type){

        switch (type){
            case SelQuestionDao.SEL_QUESTION_TYPE_NOMINATION:
                view.showQuestionOptions(false);
                break;
            case SelQuestionDao.SEL_QUESTION_TYPE_MULTI_CHOICE:
                view.showQuestionOptions(true);
                break;
            case SelQuestionDao.SEL_QUESTION_TYPE_FREE_TEXT:
                view.showQuestionOptions(false);
                break;
            default:
                break;
        }
        if(mUpdatedQuestion!=null)
            mUpdatedQuestion.setQuestionType(type);
    }

    public void updateQuestionTitle(String title){
        mUpdatedQuestion.setQuestionText(title);
    }

    public void handleSELQuestionValueChanged(SelQuestion question){
        //set the og person value
        if(mOriginalQuestion == null)
            mOriginalQuestion = question;

        if(mUpdatedQuestion == null || !mUpdatedQuestion.equals(question)) {

            if(question != null) {
                //Update the currently editing class object
                mUpdatedQuestion = question;

                view.setQuestionOnView(question);
            }
        }
    }

    public void handleClickAddOption(){
        UstadMobileSystemImpl impl = UstadMobileSystemImpl.getInstance();
        Hashtable args = new Hashtable();
        args.put(ARG_QUESTION_UID_QUESTION_DETAIL, currentQuestionUid);

        impl.go(AddQuestionOptionDialogView.VIEW_NAME, args, getContext());
    }

    public void handleClickDone(){

        mUpdatedQuestion.setQuestionActive(true);
        mUpdatedQuestion.setSelQuestionSelQuestionSetUid(currentQuestionSetUid);
        questionDao.findByUidAsync(mUpdatedQuestion.getSelQuestionUid(), new UmCallback<SelQuestion>() {
            @Override
            public void onSuccess(SelQuestion questionInDB) {
                if(questionInDB != null){
                    questionDao.updateAsync(mUpdatedQuestion, new UmCallback<Integer>() {
                        @Override
                        public void onSuccess(Integer result) {
                            view.finish();
                        }

                        @Override
                        public void onFailure(Throwable exception) {
                            exception.printStackTrace();
                        }
                    });
                }else{
                    questionDao.insertAsync(mUpdatedQuestion, new UmCallback<Long>() {
                        @Override
                        public void onSuccess(Long result) {
                            view.finish();
                        }

                        @Override
                        public void onFailure(Throwable exception) {

                        }
                    });
                }
            }

            @Override
            public void onFailure(Throwable exception) {

            }
        });

    }

    public void handleQuestionOptionEdit(long questionOptionUid){
        questionOptionDao.findByUidAsync(questionOptionUid, new UmCallback<SelQuestionOption>() {
            @Override
            public void onSuccess(SelQuestionOption result) {
                if(result != null){
                    UstadMobileSystemImpl impl = UstadMobileSystemImpl.getInstance();
                    Hashtable args = new Hashtable();
                    args.put(ARG_QUESTION_UID_QUESTION_DETAIL, result.getSelQuestionOptionQuestionUid());
                    args.put(ARG_QUESTION_OPTION_UID, result.getSelQuestionOptionUid());
                    impl.go(AddQuestionOptionDialogView.VIEW_NAME, args, getContext());

                }
            }

            @Override
            public void onFailure(Throwable exception) {
                exception.printStackTrace();
            }
        });
    }

    public void handleQuestionOptionDelete(long questionOptionUid){
        questionOptionDao.findByUidAsync(questionOptionUid, new UmCallback<SelQuestionOption>() {
            @Override
            public void onSuccess(SelQuestionOption result) {
                if(result != null){
                    result.setOptionActive(false);
                    questionOptionDao.updateAsync(result, new UmCallback<Integer>() {
                        @Override
                        public void onSuccess(Integer result) {

                        }

                        @Override
                        public void onFailure(Throwable exception) {
                            exception.printStackTrace();
                        }
                    });
                }
            }

            @Override
            public void onFailure(Throwable exception) {
                exception.printStackTrace();
            }
        });
    }

}