package com.bijoysingh.quicknote.firebase.support

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import com.bijoysingh.quicknote.Scarlet.Companion.firebase
import com.bijoysingh.quicknote.firebase.activity.FirebaseLoginActivity
import com.bijoysingh.quicknote.firebase.activity.ForgetMeActivity
import com.bijoysingh.quicknote.firebase.initFirebaseDatabase
import com.github.bijoysingh.starter.async.SimpleThreadExecutor
import com.github.bijoysingh.starter.util.ToastHelper
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.maubis.scarlet.base.config.ApplicationBase
import com.maubis.scarlet.base.config.auth.IAuthenticator
import com.maubis.scarlet.base.main.recycler.KEY_FORCE_SHOW_SIGN_IN
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class FirebaseAuthenticator() : IAuthenticator {

  var userId: String? = null

  override fun userId(context: Context): String? {
    val user = FirebaseAuth.getInstance().currentUser
    return user?.uid
  }

  override fun setup(context: Context) {
    GlobalScope.launch {
      FirebaseApp.initializeApp(context)
      try {
        userId = userId(context)
        if (userId === null) {
          return@launch
        }

        FirebaseDatabase.getInstance()
        initFirebaseDatabase(context, userId!!)
        reloadUser(context)
      } catch (exception: Exception) {
        // Don't need to do anything
      }
    }
  }

  override fun isLoggedIn(): Boolean {
    return userId !== null
  }

  override fun logout() {
    userId = null
    FirebaseAuth.getInstance().signOut()
    firebase?.logout()
  }

  override fun openLoginActivity(context: Context) = Runnable {
    context.startActivity(Intent(context, FirebaseLoginActivity::class.java))
  }

  override fun openForgetMeActivity(context: Context) = Runnable {
    context.startActivity(Intent(context, ForgetMeActivity::class.java))
  }

  private fun reloadUser(context: Context) {
    SimpleThreadExecutor.execute {
      try {
        FirebaseAuth.getInstance().currentUser?.reload()?.addOnCompleteListener {
          if (it.isSuccessful) {
            return@addOnCompleteListener
          }
          val exception = it.exception
          if (exception !== null && exception is FirebaseNetworkException) {
            return@addOnCompleteListener
          }

          logout()
          ApplicationBase.instance.store().put(KEY_FORCE_SHOW_SIGN_IN, true)
          val handler = Handler(Looper.getMainLooper())
          handler.post {
            ToastHelper.show(context, "You have been signed out of the app")
          }
        }
      } catch (e: Exception) {
        // In case somehow it fails
      }
    }
  }
}