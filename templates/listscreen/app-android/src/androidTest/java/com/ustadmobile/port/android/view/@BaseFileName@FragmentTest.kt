package com.ustadmobile.port.android.view

import androidx.core.os.bundleOf
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItem
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withTagValue
import com.toughra.ustadmobile.R
import com.ustadmobile.core.view.UstadView.Companion.ARG_ENTITY_UID
import com.ustadmobile.lib.db.entities.@Entity@
import com.ustadmobile.test.rules.SystemImplTestNavHostRule
import com.ustadmobile.test.rules.UmAppDatabaseAndroidClientRule
import org.hamcrest.Matchers.equalTo
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

class @BaseFileName@FragmentTest  {

    @JvmField
    @Rule
    var dbRule = UmAppDatabaseAndroidClientRule(useDbAsRepo = true)

    @JvmField
    @Rule
    var systemImplNavRule = SystemImplTestNavHostRule()

    @Test
    fun givenClazzPresent_whenClickOnClazz_thenShouldNavigateToClazzDetail() {
        val testEntity = @Entity@().apply {
            @Entity_VariableName@Name = "Test Name"
            @Entity_VariableName@Uid = dbRule.db.clazzDao.insert(this)
        }

        val fragmentScenario = launchFragmentInContainer<@BaseFileName@Fragment>(
                bundleOf(), themeResId = R.style.Theme_UstadTheme
        )

        fragmentScenario.onFragment {
            Navigation.setViewNavController(it.requireView(), systemImplNavRule.navController)
        }

        //Note: In order for clicking on the RecyclerView item to work, you MUST set to the tag of
        // the viewholder in the onBind method of the RecyclerView.Adapter. This must be set in the
        // method itself, not via data binding.
        onView(withId(R.id.fragment_list_recyclerview)).perform(
                actionOnItem<RecyclerView.ViewHolder>(withTagValue(equalTo(testEntity.@Entity_VariableName@Uid)),
                        click()))

        Assert.assertEquals("After clicking on item, it navigates to detail view",
                R.id.@Entity_SnakeCase@_detail_dest, systemImplNavRule.navController.currentDestination?.id)
        val currentArgs = systemImplNavRule.navController.currentDestination?.arguments
        //Note: as of 02/June/2020 arguments were missing even though they were given
    }

}