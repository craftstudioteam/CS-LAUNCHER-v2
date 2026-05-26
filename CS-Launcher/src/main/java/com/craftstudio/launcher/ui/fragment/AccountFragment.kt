package com.craftstudio.launcher.ui.fragment

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.widget.EditText
import android.widget.PopupWindow
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewbinding.ViewBinding
import com.angcyo.tablayout.DslTabLayout
import com.craftstudio.launcher.anim.AnimPlayer
import com.craftstudio.launcher.anim.animations.Animations
import com.craftstudio.launcher.R
import com.craftstudio.launcher.databinding.FragmentAccountBinding
import com.craftstudio.launcher.databinding.ItemOtherServerBinding
import com.craftstudio.launcher.databinding.ViewAddOtherServerBinding
import com.craftstudio.launcher.databinding.ViewSingleActionPopupBinding
import com.craftstudio.launcher.event.single.AccountUpdateEvent
import com.craftstudio.launcher.event.value.LocalLoginEvent
import com.craftstudio.launcher.event.value.OtherLoginEvent
import com.craftstudio.launcher.feature.accounts.AccountUtils
import com.craftstudio.launcher.feature.accounts.AccountsManager
import com.craftstudio.launcher.feature.accounts.LocalAccountUtils
import com.craftstudio.launcher.feature.accounts.LocalAccountUtils.CheckResultListener
import com.craftstudio.launcher.feature.accounts.LocalAccountUtils.Companion.checkUsageAllowed
import com.craftstudio.launcher.feature.accounts.LocalAccountUtils.Companion.openDialog
import com.craftstudio.launcher.feature.accounts.OtherLoginHelper
import com.craftstudio.launcher.feature.log.Logging
import com.craftstudio.launcher.feature.login.OtherLoginApi
import com.craftstudio.launcher.feature.login.Servers
import com.craftstudio.launcher.feature.login.Servers.Server
import com.craftstudio.launcher.setting.AllSettings
import com.craftstudio.launcher.task.Task
import com.craftstudio.launcher.task.TaskExecutors
import com.craftstudio.launcher.ui.dialog.EditTextDialog
import com.craftstudio.launcher.ui.dialog.OtherLoginDialog
import com.craftstudio.launcher.ui.dialog.TipDialog
import com.craftstudio.launcher.ui.layout.AnimRelativeLayout
import com.craftstudio.launcher.ui.subassembly.account.AccountAdapter
import com.craftstudio.launcher.ui.subassembly.account.AccountAdapter.AccountUpdateListener
import com.craftstudio.launcher.ui.subassembly.account.AccountViewWrapper
import com.craftstudio.launcher.ui.subassembly.account.SelectAccountListener
import com.craftstudio.launcher.utils.ZHTools
import com.craftstudio.launcher.utils.http.NetworkUtils
import com.craftstudio.launcher.utils.path.PathManager
import com.craftstudio.launcher.utils.stringutils.StringUtils
import com.craftstudio.launcher.Tools
import com.craftstudio.launcher.fragments.MicrosoftLoginFragment
import com.craftstudio.launcher.value.MinecraftAccount
import org.apache.commons.io.FileUtils
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.json.JSONObject
import java.io.File
import java.util.regex.Pattern


class AccountFragment : FragmentWithAnim(R.layout.fragment_account), View.OnClickListener {
    companion object {
        const val TAG = "AccountFragment"
    }

    private lateinit var binding: FragmentAccountBinding
    private lateinit var mAccountViewWrapper: AccountViewWrapper
    private val mAccountsData: MutableList<MinecraftAccount> = AccountsManager.allAccounts.toMutableList()
    private val mAccountAdapter = AccountAdapter(mAccountsData)

    private val selectAccountListener = object : SelectAccountListener {
        override fun onSelect(account: MinecraftAccount) {
            if (!isTaskRunning()) {
                AccountsManager.currentAccount = account
            } else {
                TaskExecutors.runInUIThread {
                    Toast.makeText(
                        requireActivity(),
                        R.string.tasks_ongoing,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private val mServerActionPopupWindow: PopupWindow = PopupWindow().apply {
        isFocusable = true
        isOutsideTouchable = true
    }

    private val mLocalNamePattern = Pattern.compile("[^a-zA-Z0-9_]")
    private var mOtherServerConfig: Servers? = null
    private val mOtherServerConfigFile = File(PathManager.DIR_GAME_HOME, "servers.json")
    private val mOtherServerList: MutableList<Server> = ArrayList()
    private val mOtherServerViewList: MutableList<View> = ArrayList()

    private lateinit var mProgressDialog: AlertDialog

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAccountBinding.inflate(layoutInflater)
        mAccountViewWrapper = AccountViewWrapper(binding = binding.viewAccount)
        mProgressDialog = ZHTools.createTaskRunningDialog(binding.root.context)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val context = requireActivity()

        mAccountAdapter.setAccountUpdateListener(object : AccountUpdateListener {
            override fun onViewClick(account: MinecraftAccount) {
                selectAccountListener.onSelect(account)
            }

            override fun onRefresh(account: MinecraftAccount) {
                if (!isTaskRunning()) {
                    if (!NetworkUtils.isNetworkAvailable(context)) {
                        Toast.makeText(context, R.string.account_login_no_network, Toast.LENGTH_SHORT).show()
                        return
                    }
                    AccountsManager.performLogin(context, account)
                } else {
                    Toast.makeText(context, R.string.tasks_ongoing, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onDelete(account: MinecraftAccount) {
                TipDialog.Builder(context)
                    .setTitle(R.string.generic_warning)
                    .setMessage(R.string.account_remove)
                    .setConfirm(R.string.generic_delete)
                    .setWarning()
                    .setConfirmClickListener {
                        val accountFile =
                            File(PathManager.DIR_ACCOUNT_NEW, account.uniqueUUID)
                        val userSkinFile =
                            File(PathManager.DIR_USER_SKIN, account.uniqueUUID + ".png")
                        if (accountFile.exists()) FileUtils.deleteQuietly(accountFile)
                        if (userSkinFile.exists()) FileUtils.deleteQuietly(userSkinFile)
                        reloadAccounts()
                    }.showDialog()
            }
        })

        binding.apply {
            accountsRecycler.layoutManager = LinearLayoutManager(context)
            accountsRecycler.setLayoutAnimation(
                LayoutAnimationController(
                    AnimationUtils.loadAnimation(
                        context,
                        R.anim.fade_downwards
                    )
                )
            )
            accountsRecycler.adapter = mAccountAdapter

            accountTypeTab.observeIndexChange { _, toIndex, _, fromUser ->
                fun nonMicrosoftLogin(message: Int, login: () -> Unit) {
                    checkUsageAllowed(object : CheckResultListener {
                        override fun onUsageAllowed() {
                            login()
                        }

                        override fun onUsageDenied() {
                            if (!AllSettings.localAccountReminders.getValue()) {
                                login()
                            } else {
                                openDialog(
                                    context,
                                    TipDialog.OnConfirmClickListener { checked ->
                                        LocalAccountUtils.saveReminders(checked)
                                        login()
                                    },
                                    getString(message) + getString(
                                        R.string.account_purchase_minecraft_account_tip
                                    ),
                                    R.string.account_no_microsoft_account_continue
                                )
                            }
                        }
                    })
                }

                if (fromUser) { //需要判断是否为用户手动点击的，否则会一直进入微软登录界面
                    when (toIndex) {
                        //微软账户
                        0 -> ZHTools.swapFragmentWithAnim(
                            this@AccountFragment,
                            MicrosoftLoginFragment::class.java,
                            MicrosoftLoginFragment.TAG,
                            null
                        )
                        //离线账户
                        1 -> {
                            nonMicrosoftLogin(
                                R.string.account_no_microsoft_account_local
                            ) { localLogin() }
                        }
                        //外置账户
                        else -> {
                            nonMicrosoftLogin(
                                R.string.account_no_microsoft_account_other
                            ) { otherLogin(toIndex - 2) /* Server索引需要从0开始 */ }
                        }
                    }
                }
            }

            addServer.setOnClickListener(this@AccountFragment)
            returnButton.setOnClickListener(this@AccountFragment)
        }

        reloadAccounts()
        refreshOtherServer()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun reloadRecyclerView() {
        this.mAccountsData.clear()
        mAccountsData.addAll(AccountsManager.allAccounts)

        this.mAccountAdapter.notifyDataSetChanged()
        binding.accountsRecycler.scheduleLayoutAnimation()
    }

    private fun reloadAccounts() {
        Task.runTask {
            AccountsManager.reload()
        }.ended(TaskExecutors.getAndroidUI()) {
            reloadRecyclerView()
            mAccountViewWrapper.refreshAccountInfo()
        }.execute()
    }

    private fun SpannableString.spanText(start: Int, end: Int, what: Any) {
        this.setSpan(what, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    private fun localLogin() {
        fun startLogin(name: String) {
            EventBus.getDefault().post(LocalLoginEvent(name.trim()))
        }

        EditTextDialog.Builder(requireActivity())
            .setTitle(R.string.account_login_local_name)
            .setConfirmText(R.string.generic_login)
            .setEmptyErrorText(R.string.account_local_account_empty)
            .setAsRequired()
            .setConfirmListener { editText, _ ->
                val string = editText.text.toString()
                if (string.length <= 2 || string.length > 16 || mLocalNamePattern.matcher(string).find()) {
                    TipDialog.Builder(requireContext())
                        .setTitle(R.string.generic_warning)
                        .setMessage(R.string.account_local_account_invalid)
                        .setWarning()
                        .setTextBeautifier { _, messageText ->
                            val text = messageText.text.toString()
                            val startTag = "[RED;BOLD]"
                            val endTag = "[/RED;BOLD]"

                            val startIndex = text.indexOf(startTag)
                            val endIndex = text.indexOf(endTag)

                            if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
                                val styledText = text.substring(startIndex + startTag.length, endIndex)
                                val plainText = text.replace(startTag, "").replace(endTag, "")
                                val adjustedEndIndex = startIndex + styledText.length

                                val spannableString = SpannableString(plainText)
                                spannableString.spanText(startIndex, adjustedEndIndex, ForegroundColorSpan(Color.RED))
                                spannableString.spanText(startIndex, adjustedEndIndex, StyleSpan(Typeface.BOLD))

                                messageText.text = spannableString
                            }
                        }
                        .setCenterMessage(false)
                        .setConfirmClickListener { startLogin(string) }
                        .setCancelable(false)
                        .setConfirmButtonCountdown(3000L)
                        .showDialog()
                } else startLogin(string)

                true
            }.showDialog()
    }

    private fun otherLogin(index: Int) {
        val server = mOtherServerList[index]
        OtherLoginDialog(requireActivity(), server,
            object : OtherLoginHelper.OnLoginListener {
                override fun onLoading() {
                    mProgressDialog.show()
                }

                override fun unLoading() {
                    mProgressDialog.dismiss()
                }

                override fun onSuccess(account: MinecraftAccount) {
                    EventBus.getDefault().post(OtherLoginEvent(account))
                }

                override fun onFailed(error: String) {
                    mProgressDialog.dismiss()

                    TipDialog.Builder(requireActivity())
                        .setTitle(R.string.generic_warning)
                        .setMessage(getString(R.string.other_login_error) + error)
                        .setWarning()
                        .setCancel(android.R.string.copy)
                        .setCancelClickListener {
                            StringUtils.copyText(
                                "error",
                                error,
                                requireActivity()
                            )
                        }
                        .showDialog()
                }
            }).show()
    }

    private fun refreshOtherServer() {
        Task.runTask {
            mOtherServerList.clear()
            if (mOtherServerConfigFile.exists()) {
                runCatching {
                    val serverConfig = Tools.GLOBAL_GSON.fromJson(
                        Tools.read(mOtherServerConfigFile.absolutePath),
                        Servers::class.java
                    )
                    mOtherServerConfig = serverConfig
                    serverConfig.server.forEach { server ->
                        mOtherServerList.add(server)
                    }
                }
            }
        }.ended(TaskExecutors.getAndroidUI()) {
            //将外置服务器添加到账号类别选择栏上
            mOtherServerViewList.forEach { view ->
                binding.accountTypeTab.removeView(view)
            }
            mOtherServerViewList.clear()

            val activity = requireActivity()
            val layoutInflater = activity.layoutInflater

            fun createView(server: Server): AnimRelativeLayout {
                val p8 = Tools.dpToPx(8f).toInt()
                val view = ItemOtherServerBinding.inflate(layoutInflater)
                view.text.text = server.serverName
                view.root.setOnLongClickListener { v ->
                    refreshActionPopupWindow(v, ViewSingleActionPopupBinding.inflate(LayoutInflater.from(activity)).apply {
                        icon.setImageDrawable(
                            ContextCompat.getDrawable(requireActivity(), R.drawable.ic_menu_delete_forever)
                        )
                        text.setText(R.string.generic_delete)
                        text.setOnClickListener {
                            deleteOtherServer(server)
                            mServerActionPopupWindow.dismiss()
                        }
                    })
                    true
                }
                view.root.layoutParams = DslTabLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                return view.root.apply {
                    setPadding(p8, 0, p8, 0)
                }
            }

            mOtherServerList.forEach { server ->
                val view = createView(server)
                mOtherServerViewList.add(view)
                binding.accountTypeTab.addView(view)
            }
        }.execute()
    }

    private fun showServerTypeSelectDialog(stringId: Int, type: Int) {
        EditTextDialog.Builder(requireActivity())
            .setTitle(stringId)
            .setAsRequired()
            .setConfirmListener { editText, _ ->
                addOtherServer(editText, type)
                true
            }.showDialog()
    }

    private fun checkServerConfig() {
        mOtherServerConfig ?: run {
            val servers = Servers()
            servers.server = ArrayList()
            mOtherServerConfig = servers
        }
    }

    private fun addOtherServer(editText: EditText, type: Int) {
        Task.runTask {
            val editString = editText.text.toString()
            val serverUrl =
                if (type == 0) AccountUtils.tryGetFullServerUrl(editString) else editString
            OtherLoginApi.getServeInfo(
                requireActivity(),
                if (type == 0) serverUrl else "https://auth.mc-user.com:233/$serverUrl"
            )?.let { data ->
                val server = Server()
                JSONObject(data).optJSONObject("meta")?.let { meta ->
                    server.serverName = meta.optString("serverName")
                    server.baseUrl = serverUrl
                    if (type == 0) {
                        server.register =
                            meta.optJSONObject("links")?.optString("register") ?: ""
                    } else {
                        server.baseUrl = "https://auth.mc-user.com:233/$serverUrl"
                        server.register = "https://login.mc-user.com:233/$serverUrl"
                    }
                    checkServerConfig()
                    mOtherServerConfig?.server?.apply addServer@{
                        forEach {
                            //确保服务器不重复
                            if (it.baseUrl == server.baseUrl) return@addServer
                        }
                        add(server)
                    }
                    Tools.write(
                        mOtherServerConfigFile.absolutePath,
                        Tools.GLOBAL_GSON.toJson(mOtherServerConfig, Servers::class.java)
                    )
                }
            }
        }.beforeStart(TaskExecutors.getAndroidUI()) {
            mProgressDialog.show()
        }.ended(TaskExecutors.getAndroidUI()) {
            refreshOtherServer()
            mProgressDialog.dismiss()
        }.onThrowable { e ->
            Logging.e("Add Other Server", Tools.printToString(e))
        }.execute()
    }

    private fun deleteOtherServer(server: Server) {
        TipDialog.Builder(requireActivity())
            .setTitle(getString(R.string.account_remove_login_type_title, server.serverName))
            .setMessage(R.string.account_remove_login_type_message)
            .setWarning()
            .setConfirmClickListener {
                checkServerConfig()
                mOtherServerConfig?.server?.remove(server)
                Tools.write(
                    mOtherServerConfigFile.absolutePath,
                    Tools.GLOBAL_GSON.toJson(mOtherServerConfig, Servers::class.java)
                )
                refreshOtherServer()
            }.showDialog()
    }

    private fun refreshActionPopupWindow(anchorView: View, binding: ViewBinding) {
        mServerActionPopupWindow.apply {
            binding.root.measure(0, 0)
            this.contentView = binding.root
            this.width = binding.root.measuredWidth
            this.height = binding.root.measuredHeight
            showAsDropDown(anchorView)
        }
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun event(event: AccountUpdateEvent) {
        mAccountViewWrapper.refreshAccountInfo()
        reloadRecyclerView()
    }

    override fun onClick(v: View) {
        val activity = requireActivity()
        binding.apply {
            when (v) {
                returnButton -> ZHTools.onBackPressed(activity)
                addServer -> {
                    refreshActionPopupWindow(v, ViewAddOtherServerBinding.inflate(LayoutInflater.from(activity)).apply {
                        val onClickListener = View.OnClickListener { v1 ->
                            when(v1) {
                                addOtherServer -> showServerTypeSelectDialog(R.string.other_login_yggdrasil_api, 0)
                                addUniformPass -> showServerTypeSelectDialog(R.string.other_login_32_bit_server, 1)
                            }
                            mServerActionPopupWindow.dismiss()
                        }
                        addOtherServer.setOnClickListener(onClickListener)
                        addUniformPass.setOnClickListener(onClickListener)
                    })
                }
                else -> {}
            }
        }
    }

    override fun slideIn(animPlayer: AnimPlayer) {
        binding.apply {
            animPlayer.apply(AnimPlayer.Entry(operationLayout, Animations.BounceInLeft))
                .apply(AnimPlayer.Entry(accountTypeLayout, Animations.BounceInDown))
                .apply(AnimPlayer.Entry(accountsRecycler, Animations.BounceInUp))
        }
    }

    override fun slideOut(animPlayer: AnimPlayer) {
        binding.apply {
            animPlayer.apply(AnimPlayer.Entry(operationLayout, Animations.FadeOutRight))
                .apply(AnimPlayer.Entry(accountTypeLayout, Animations.FadeOutUp))
                .apply(AnimPlayer.Entry(accountsRecycler, Animations.FadeOutDown))
        }
    }
}