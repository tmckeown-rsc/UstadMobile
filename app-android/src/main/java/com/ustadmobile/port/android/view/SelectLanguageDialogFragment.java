package com.ustadmobile.port.android.view;

import android.app.Dialog;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.toughra.ustadmobile.R;
import com.ustadmobile.core.controller.SelectLanguageDialogPresenter;
import com.ustadmobile.core.view.DismissableDialog;
import com.ustadmobile.core.view.SelectLanguageDialogView;
import com.ustadmobile.port.android.util.UMAndroidUtil;

import java.util.Locale;
import java.util.Objects;

import io.reactivex.annotations.NonNull;

public class SelectLanguageDialogFragment extends UstadDialogFragment implements
        SelectLanguageDialogView, DismissableDialog {

    AlertDialog dialog;
    View rootView;

    private SelectLanguageDialogPresenter mPresenter;
    //Context (Activity calling this)
    private Context mAttachedContext;
    Toolbar toolbar;
    TextView english, dari, pashto;




    public static SelectLanguageDialogFragment newInstance(){
        SelectLanguageDialogFragment fragment = new SelectLanguageDialogFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @android.support.annotation.NonNull
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        LayoutInflater inflater =
                (LayoutInflater) Objects.requireNonNull(getContext()).getSystemService(
                        Context.LAYOUT_INFLATER_SERVICE);
        assert inflater != null;

        rootView = inflater.inflate(R.layout.fragment_select_language_dialog, null);

        english = rootView.findViewById(R.id.fragment_select_language_dialog_english);
        dari = rootView.findViewById(R.id.fragment_select_language_dialog_dari);
        pashto = rootView.findViewById(R.id.fragment_select_language_dialog_pashto);


        english.setOnClickListener(v -> handleClickEnglish());
        dari.setOnClickListener(v -> handleClickDari());
        pashto.setOnClickListener(v -> handleClickPashto());

        mPresenter = new SelectLanguageDialogPresenter(getContext(),
                UMAndroidUtil.bundleToHashtable(getArguments()), this);
        mPresenter.onCreate(UMAndroidUtil.bundleToHashtable(savedInstanceState));

        //Set any view components and its listener (post presenter work)


        AlertDialog.Builder builder = new AlertDialog.Builder(Objects.requireNonNull(getContext()));
        builder.setTitle(R.string.choose_language);
        builder.setView(rootView);
        dialog = builder.create();

        return dialog;

    }

    private void handleClickEnglish(){
        Locale englishLocale = Locale.forLanguageTag("en");
        //TODO
        Locale.setDefault(englishLocale);
        Configuration config = new Configuration();
        // TODO fix deprecation issues
        config.locale = englishLocale;
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());

    }
    private void handleClickDari(){
        Locale englishLocale = Locale.forLanguageTag("dr");
        //TODO
        Locale.setDefault(englishLocale);
        Configuration config = new Configuration();
        // TODO fix deprecation issues
        config.locale = englishLocale;
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
        //TODO
    }
    private void handleClickPashto(){
        Locale englishLocale = Locale.forLanguageTag("ps");
        //TODO
        Locale.setDefault(englishLocale);
        Configuration config = new Configuration();
        // TODO fix deprecation issues
        config.locale = englishLocale;
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
        //TODO
    }

    @Override
    public void onAttach(Context context){
        super.onAttach(context);
        this.mAttachedContext = context;
    }

    @Override
    public void onDetach(){
        super.onDetach();
        this.mAttachedContext = null;
    }

    @Override
    public void finish(){
        dialog.dismiss();
    }

}