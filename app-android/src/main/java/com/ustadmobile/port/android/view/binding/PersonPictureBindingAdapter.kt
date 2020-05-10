package com.ustadmobile.port.android.view.binding

import android.net.Uri
import android.view.View
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.squareup.picasso.Picasso
import com.toughra.ustadmobile.R
import com.ustadmobile.core.impl.UmAccountManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import java.io.File

@BindingAdapter(value=["personPicturePersonUid", "personPictureVisibilityGoneIfNoPicture"], requireAll = false)
fun ImageView.setPersonPicture(personPicturePersonUid: Long?, personPictureVisibilityGoneIfNoPicture: Boolean?){
    val personUid = personPicturePersonUid ?: 0L

    //cancel any previous image load jobs
    (getTag(R.id.tag_imageloadjob) as? Job)?.cancel()
    setTag(R.id.tag_imageloadjob, null)

    val imageLoadJob = GlobalScope.async(Dispatchers.Main) {
        val personPictureDaoRepo = UmAccountManager.getRepositoryForActiveAccount(context)
                .personPictureDao
        val personPictureDao = UmAccountManager.getActiveDatabase(context)
                .personPictureDao


        val personPictureLocal = personPictureDao.findByPersonUidAsync(personUid)
        val imgPath = if(personPictureLocal != null) personPictureDaoRepo.getAttachmentPath(personPictureLocal) else null

        when {
            !imgPath.isNullOrEmpty() -> {
                if(personPictureVisibilityGoneIfNoPicture == true)
                    visibility = View.VISIBLE

                Picasso.get()
                        .load(Uri.fromFile(File(imgPath)))
                        .noFade()
                        .fit()
                        .centerCrop()
                        .into(this@setPersonPicture)
            }
            personPictureVisibilityGoneIfNoPicture == true -> visibility = View.GONE
            else -> setImageResource(R.drawable.ic_person_black_new_24dp)
        }

        setTag(R.id.tag_imageloadjob, null)
    }

    setTag(R.id.tag_imageloadjob, imageLoadJob)
}
