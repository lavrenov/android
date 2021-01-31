package com.example.developerslife.ui.main

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.common.Priority
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import com.bumptech.glide.Glide
import com.example.developerslife.R
import org.json.JSONObject
import kotlin.math.ceil


/**
 * A placeholder fragment containing a simple view.
 */
class PlaceholderFragment : Fragment() {

    private lateinit var pageViewModel: PageViewModel
    private var totalItems: Int = 0
    private var curPage: Int = 0
    private var perPage: Int = 5
    private var curItem: Int = 0
    private var section: String = "latest"
    private var pref: SharedPreferences? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pageViewModel = ViewModelProvider(this).get(PageViewModel::class.java).apply {
            setIndex(arguments?.getInt(ARG_SECTION_NUMBER) ?: 1)
        }

        when (arguments?.getInt(ARG_SECTION_NUMBER) ?: 1) {
            1 -> section = "latest"
            2 -> section = "top"
            3 -> section = "hot"
        }
        pref = context?.getSharedPreferences(section, Context.MODE_PRIVATE)
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_main, container, false)
        val backBtn: Button = root.findViewById(R.id.back_button)
        val nextBtn: Button = root.findViewById(R.id.next_button)
        val retryBtn: Button = root.findViewById(R.id.retry_button)

        loadImage(root)

        backBtn.setOnClickListener {
            curItem--
            curPage = ceil((curItem / perPage).toDouble()).toInt()
            if (curItem == 0) {
                backBtn.isEnabled = false
            }
            if (curPage < totalItems - 1) {
                nextBtn.isEnabled = true
            }
            loadImage(root)
        }

        nextBtn.setOnClickListener {
            curItem++
            curPage = ceil((curItem / perPage).toDouble()).toInt()
            if (curItem > 0) {
                backBtn.isEnabled = true
            }
            if (curItem >= totalItems - 1) {
                nextBtn.isEnabled = false
            }
            loadImage(root)
        }

        retryBtn.setOnClickListener {
            loadImage(root)
        }

        return root
    }

    private fun loadImage(root: View) {
        val textView: TextView = root.findViewById(R.id.section_label)
        val imageView: ImageView = root.findViewById(R.id.section_img)
        val btnLayout: LinearLayout = root.findViewById(R.id.btn_layout)
        val retryLayout: LinearLayout = root.findViewById(R.id.retry_layout)

        val data = pref?.getString(curPage.toString(), "")

        if (data != "") {
            val response = JSONObject(data.toString())
            viewImage(root, response)
        } else {
            AndroidNetworking.initialize(context)
            AndroidNetworking.get("https://developerslife.ru/$section/$curPage")
                .addQueryParameter("json", "true")
                .setPriority(Priority.MEDIUM)
                .build()
                .getAsJSONObject(object : JSONObjectRequestListener {
                    override fun onResponse(response: JSONObject) {
                        val editor = pref?.edit()
                        editor?.putString(curPage.toString(), response.toString())
                        editor?.apply()

                        viewImage(root, response)
                    }

                    override fun onError(error: ANError) {
                        btnLayout.visibility = View.GONE
                        imageView.visibility = View.GONE
                        retryLayout.visibility = View.VISIBLE
                        textView.text = resources.getString(R.string.error)
                    }
                })
        }
    }

    private fun viewImage(root: View, response: JSONObject) {
        val textView: TextView = root.findViewById(R.id.section_label)
        val imageView: ImageView = root.findViewById(R.id.section_img)
        val btnLayout: LinearLayout = root.findViewById(R.id.btn_layout)
        val retryLayout: LinearLayout = root.findViewById(R.id.retry_layout)
        val fragment: Fragment = this

        retryLayout.visibility = View.GONE

        totalItems = (response["totalCount"] as Int)
        if (totalItems > 0) {
            imageView.visibility = View.VISIBLE
            btnLayout.visibility = View.VISIBLE
            val jsonArray = response.optJSONArray("result")
            if (jsonArray != null) {
                val jsonObject =
                    jsonArray.getJSONObject(curItem - (perPage * curPage))
                val description = jsonObject.optString("description")
                val gifUrl = jsonObject.optString("gifURL")

                Glide.with(fragment)
                    .load(gifUrl.replace("http", "https"))
                    .placeholder(R.drawable.loading)
                    .into(imageView)

                textView.text = description
            }
        } else {
            btnLayout.visibility = View.GONE
            imageView.visibility = View.GONE
            textView.text = resources.getString(R.string.no_images)
        }
    }

    companion object {
        private const val ARG_SECTION_NUMBER = "section_number"
        @JvmStatic
        fun newInstance(sectionNumber: Int): PlaceholderFragment {
            return PlaceholderFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_SECTION_NUMBER, sectionNumber)
                }
            }
        }
    }
}