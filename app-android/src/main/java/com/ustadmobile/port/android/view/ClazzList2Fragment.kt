package com.ustadmobile.port.android.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.toughra.ustadmobile.R
import com.toughra.ustadmobile.databinding.ItemClazzlist2ClazzBinding
import com.ustadmobile.core.controller.ClazzList2Presenter
import com.ustadmobile.core.controller.UstadListPresenter
import com.ustadmobile.core.impl.UMAndroidUtil
import com.ustadmobile.core.impl.UmAccountManager
import com.ustadmobile.core.impl.UstadMobileSystemImpl
import com.ustadmobile.core.util.MessageIdOption
import com.ustadmobile.core.view.ClazzList2View
import com.ustadmobile.lib.db.entities.Clazz
import com.ustadmobile.lib.db.entities.ClazzWithNumStudents
import com.ustadmobile.port.android.view.ext.navigateToEditEntity
import com.ustadmobile.port.android.view.util.NewItemRecyclerViewAdapter
import com.ustadmobile.port.android.view.util.SelectablePagedListAdapter

class ClazzList2Fragment(): UstadListViewFragment<Clazz, ClazzWithNumStudents>(),
        ClazzList2View, MessageIdSpinner.OnMessageIdOptionSelectedListener, View.OnClickListener{

    private var mPresenter: ClazzList2Presenter? = null

    override val listPresenter: UstadListPresenter<*, in ClazzWithNumStudents>?
        get() = mPresenter

    class ClazzList2RecyclerAdapter(var presenter: ClazzList2Presenter?): SelectablePagedListAdapter<ClazzWithNumStudents, ClazzList2RecyclerAdapter.ClazzList2ViewHolder>(DIFF_CALLBACK) {

        class ClazzList2ViewHolder(val itemBinding: ItemClazzlist2ClazzBinding): RecyclerView.ViewHolder(itemBinding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClazzList2ViewHolder {
            val itemBinding = ItemClazzlist2ClazzBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ClazzList2ViewHolder(itemBinding)
        }

        override fun onBindViewHolder(holder: ClazzList2ViewHolder, position: Int) {
            holder.itemBinding.clazz = getItem(position)
            holder.itemBinding.presenter = presenter
        }

        override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
            super.onDetachedFromRecyclerView(recyclerView)
            presenter = null
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        dbRepo = UmAccountManager.getRepositoryForActiveAccount(requireContext())
        mPresenter = ClazzList2Presenter(requireContext(), UMAndroidUtil.bundleToMap(arguments),
                this, this, UstadMobileSystemImpl.instance,
                UmAccountManager.getActiveDatabase(requireContext()),
                UmAccountManager.getRepositoryForActiveAccount(requireContext()),
                UmAccountManager.activeAccountLiveData)
        mNewItemRecyclerViewAdapter = NewItemRecyclerViewAdapter(this,
            requireContext().getString(R.string.create_new, R.string.clazz))
        mDataRecyclerViewAdapter = ClazzList2RecyclerAdapter(mPresenter)

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mPresenter = null
        dbRepo = null
    }


    override fun onClick(v: View?) {
        if(view?.id == R.id.item_createnew_layout)
            navigateToEditEntity(null, R.id.clazz_edit_dest, Clazz::class.java)
    }

    override fun onMessageIdOptionSelected(view: AdapterView<*>?, messageIdOption: MessageIdOption) {
        mPresenter?.handleClickSortOrder(messageIdOption)
    }

    override fun onNoMessageIdOptionSelected(view: AdapterView<*>?) {
        //do nothing
    }

    override fun onResume() {
        super.onResume()
        mActivityWithFab?.activityFloatingActionButton?.text = requireContext().getText(R.string.clazz)
    }

    override val displayTypeRepo: Any?
        get() = dbRepo?.clazzDao

    companion object {
        val DIFF_CALLBACK: DiffUtil.ItemCallback<ClazzWithNumStudents> = object
            : DiffUtil.ItemCallback<ClazzWithNumStudents>() {
            override fun areItemsTheSame(oldItem: ClazzWithNumStudents,
                                         newItem: ClazzWithNumStudents): Boolean {
                return oldItem.clazzUid == newItem.clazzUid
            }

            override fun areContentsTheSame(oldItem: ClazzWithNumStudents,
                                            newItem: ClazzWithNumStudents): Boolean {
                return oldItem == newItem
            }
        }
    }
}