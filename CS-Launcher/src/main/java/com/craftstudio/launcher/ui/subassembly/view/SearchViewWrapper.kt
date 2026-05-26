package com.craftstudio.launcher.ui.subassembly.view

import android.view.View
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.craftstudio.launcher.R
import com.petterp.floatingx.assist.FxGravity
import com.petterp.floatingx.assist.helper.FxScopeHelper
import com.petterp.floatingx.listener.IFxViewLifecycle
import com.petterp.floatingx.listener.control.IFxScopeControl
import com.petterp.floatingx.view.FxViewHolder

class SearchViewWrapper(private val fragment: Fragment) {
    private lateinit var mSearchEditText: EditText
    private var searchListener: SearchListener? = null
    private var showSearchResultsListener: ShowSearchResultsListener? = null
    private var searchAsynchronousUpdatesListener: SearchAsynchronousUpdatesListener? = null
    private var isShow = false

    private var scopeFx: IFxScopeControl? = null

    private fun getWindow(): IFxScopeControl {
        return FxScopeHelper.Builder().apply {
            setLayout(R.layout.view_search)
            setEnableEdgeAdsorption(false)
            addViewLifecycle(object : IFxViewLifecycle {
                override fun initView(holder: FxViewHolder) {
                    mSearchEditText = holder.getView(R.id.edit_text)
                    val caseSensitive = holder.getView<CheckBox>(R.id.case_sensitive)
                    val searchCountText = holder.getView<TextView>(R.id.text)

                    holder.getView<ImageButton>(R.id.search_button).setOnClickListener {
                        search(searchCountText, caseSensitive.isChecked)
                    }
                    holder.getView<CheckBox>(R.id.show_search_results_only).setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
                        showSearchResultsListener?.apply { onSearch(isChecked) }
                        if (mSearchEditText.getText().toString().isNotEmpty()) search(searchCountText, caseSensitive.isChecked)
                    }
                }
            })
            setGravity(FxGravity.TOP_OR_CENTER)
        }.build().toControl(fragment)
    }

    private fun search(searchCountText: TextView, caseSensitive: Boolean) {
        val searchCount: Int
        val string = mSearchEditText.text.toString()
        searchListener?.apply {
            searchCount = onSearch(string, caseSensitive)
            searchCountText.text = searchCountText.context.getString(R.string.search_count, searchCount)
            if (searchCount != 0) searchCountText.visibility = View.VISIBLE
            return
        }
        searchAsynchronousUpdatesListener?.apply { onSearch(searchCountText, string, caseSensitive) }
    }

    fun setSearchListener(listener: SearchListener?) {
        this.searchListener = listener
    }

    fun setAsynchronousUpdatesListener(listener: SearchAsynchronousUpdatesListener?) {
        this.searchAsynchronousUpdatesListener = listener
    }

    fun setShowSearchResultsListener(listener: ShowSearchResultsListener?) {
        this.showSearchResultsListener = listener
    }

    fun isVisible() = isShow

    fun setVisibility() {
        isShow = !isShow
        setVisibility(isShow)
    }

    fun setVisibility(visible: Boolean) {
        if (visible) {
            scopeFx ?: run {
                scopeFx = getWindow().apply {
                    show()
                }
            }
        } else {
            scopeFx?.cancel()
            scopeFx = null
        }
    }

    interface SearchListener {
        fun onSearch(string: String?, caseSensitive: Boolean): Int
    }

    interface SearchAsynchronousUpdatesListener {
        fun onSearch(searchCount: TextView?, string: String?, caseSensitive: Boolean)
    }

    interface ShowSearchResultsListener {
        fun onSearch(show: Boolean)
    }
}
