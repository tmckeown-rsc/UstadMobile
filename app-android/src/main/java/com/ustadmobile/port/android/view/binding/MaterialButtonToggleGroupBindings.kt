package com.ustadmobile.port.android.view.binding

import androidx.databinding.BindingAdapter
import androidx.databinding.InverseBindingAdapter
import androidx.databinding.InverseBindingListener
import com.google.android.material.button.MaterialButtonToggleGroup
import com.toughra.ustadmobile.R

//Map = Map of Value -> View Id
@BindingAdapter(value = ["messageGroupOptions", "messageGroupSelectedId"])
fun MaterialButtonToggleGroup.setMessageOptions(messageGroupOptions: Map<Int, Int>?, messageGroupSelectedId: Int?) {
    if(messageGroupOptions == null || messageGroupSelectedId == null)
        return

    check(messageGroupOptions[messageGroupSelectedId] ?: 0)
    setTag(R.id.tag_button_toggle_group_map, messageGroupOptions)
}

@InverseBindingAdapter(attribute = "messageGroupSelectedId")
@SuppressWarnings("UncheckedCast")
fun MaterialButtonToggleGroup.getSelectedOptionId(): Int {
    val map = getTag(R.id.tag_button_toggle_group_map) as? Map<Int, Int>
    return map?.entries?.firstOrNull { it.value == this.checkedButtonId }?.key ?: -1
}

@BindingAdapter("messageGroupSelectedIdAttrChanged")
fun MaterialButtonToggleGroup.setSelectedOptionChangedListener(inverseBindingListener: InverseBindingListener) {
    addOnButtonCheckedListener { group, checkedId, isChecked ->
        if(isChecked)
            inverseBindingListener.onChange()
    }
}
