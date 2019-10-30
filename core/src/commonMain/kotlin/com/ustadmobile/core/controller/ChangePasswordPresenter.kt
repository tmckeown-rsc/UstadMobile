package com.ustadmobile.core.controller


import com.ustadmobile.core.db.UmAppDatabase
import com.ustadmobile.core.db.dao.PersonAuthDao
import com.ustadmobile.core.db.dao.PersonDao
import com.ustadmobile.core.generated.locale.MessageID
import com.ustadmobile.core.impl.UmAccountManager
import com.ustadmobile.core.networkmanager.defaultHttpClient
import com.ustadmobile.core.view.ChangePasswordView
import com.ustadmobile.lib.db.entities.Person
import com.ustadmobile.lib.db.entities.PersonAuth
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.response.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.takeFrom
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


/**
 * Presenter for ChangePassword view
 */
class ChangePasswordPresenter(context: Any,
                              arguments: Map<String, String>?,
                              view: ChangePasswordView)
    : UstadBaseController<ChangePasswordView>(context, arguments!!, view) {

    internal var repository: UmAppDatabase
    private val personDao: PersonDao
    private val personAuthDao: PersonAuthDao
    private var currentPerson: Person? = null
    private var currentPersonAuth: PersonAuth? = null
    private var loggedInPersonUid = 0L
    var currentPassword: String? = null
    var updatePassword: String? = null
    var updatePasswordConfirm: String? = null

    init {
        repository = UmAccountManager.getRepositoryForActiveAccount(context)
        personDao = repository.personDao
        personAuthDao = repository.personAuthDao
        val cp = UmAccountManager.getActiveAccount(context)
        if (cp != null) {
            loggedInPersonUid = cp.personUid
        }
    }

    override fun onCreate(savedState: Map<String, String?>?) {
        super.onCreate(savedState)

        if (loggedInPersonUid != 0L) {
            GlobalScope.launch {
                var result = personDao.findByUidAsync(loggedInPersonUid)
                currentPerson = result
                if (currentPerson != null) {
                    var personAuth = personAuthDao.findByUidAsync(loggedInPersonUid)
                    if (personAuth == null) {
                        currentPersonAuth = PersonAuth()
                        currentPersonAuth!!.personAuthUid = loggedInPersonUid
                    }else{
                        currentPersonAuth = personAuth
                    }
                }
            }
        }
    }

    fun handleClickSave() {

        if (updatePassword != null && !updatePassword!!.isEmpty() && updatePasswordConfirm != null &&
                !updatePasswordConfirm!!.isEmpty() && currentPersonAuth != null && currentPerson != null) {
            if (updatePassword != updatePasswordConfirm) {
                view.sendMessage(MessageID.passwords_dont_match)
                return
            }

            currentPersonAuth!!.passwordHash = PersonAuthDao.encryptThisPassword(updatePassword!!)

            GlobalScope.launch {
                personAuthDao.updateAsync(currentPersonAuth!!)

                //Update on server
                try {
                    val serverUrl = UmAccountManager.getActiveEndpoint(context)
                    val resetPasswordResponse = defaultHttpClient().get<HttpResponse>()
                    {
                        url {
                            takeFrom(serverUrl!!)
                            encodedPath = "${encodedPath}UmAppDatabase/PersonAuthDao/resetPassword"
                        }
                        parameter("p0", currentPerson!!.personUid)
                        parameter("p1", updatePassword!!)
                        parameter("p2", currentPerson!!.personUid)
                    }

                    if(resetPasswordResponse.status == HttpStatusCode.OK) {

                        try {
                            personAuthDao.updateAsync(currentPersonAuth!!)
                            view.finish()

                        } catch (e: Exception) {
                            println(e!!.message)
                            view.sendMessage(MessageID.unable_to_update_password)
                        }


                    }else {
                        println("nope")
                    }
                } catch (e: Exception) {
                    print("oops")
                }

//                val resetP = personAuthDao.resetPassword(currentPerson!!.personUid, updatePassword!!, currentPerson!!.personUid)
//                if(resetP > 0) {
//                    try {
//                        personAuthDao.updateAsync(currentPersonAuth!!)
//                        view.finish()
//
//                    } catch (e: Exception) {
//                        println(e!!.message)
//                        view.sendMessage(MessageID.unable_to_update_password)
//                    }
//                }else{
//                    view.sendMessage(MessageID.unable_to_update_password)
//                }
            }
        }
    }



}
