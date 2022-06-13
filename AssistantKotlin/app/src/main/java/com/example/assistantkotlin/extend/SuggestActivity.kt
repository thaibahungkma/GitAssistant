package com.example.assistantkotlin.extend

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.assistantkotlin.R
import com.example.assistantkotlin.adapter.ListSuggestAdapter
import com.example.assistantkotlin.assistant.HomeActivity
import com.example.assistantkotlin.model.ModelSuggest
import kotlinx.android.synthetic.main.activity_suggest.*
import kotlinx.android.synthetic.main.tool_bar_suggest.*

class SuggestActivity : AppCompatActivity() {
    private lateinit var suggestArrayList: ArrayList<ModelSuggest>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_suggest)

        val imgId = intArrayOf(
            R.drawable.ic_sugg_chat_24,
            R.drawable.ic_baseline_bluetooth_24,
            R.drawable.ic_baseline_flashlight_on_24,
            R.drawable.ic_sugg_phone,
            R.drawable.ic_contact_phone_24,
            R.drawable.ic_cached_24,
            R.drawable.ic_chat_bubble,
            R.drawable.iconsfacebook,
            R.drawable.iconsyoutube,
            R.drawable.iconsyoutube,
            R.drawable.ic_baseline_date_range_24,
            R.drawable.ic_baseline_access_time_24,
            R.drawable.iconsweather,
            R.drawable.icons8note,
            R.drawable.icons8note,
            R.drawable.icons8reminder64,
            R.drawable.iconalarmclock,
            R.drawable.icongoogle,
            R.drawable.iconsyoutube,
            R.drawable.ic_baseline_calculate_24,
            R.drawable.ic_baseline_question_mark_24,
            R.drawable.ic_baseline_question_mark_24,
        )
        val name = arrayOf(
            "Giới thiệu về bạn",
            "Bật/Tắt Bluetooth",
            "Bật/Tắt đèn pin",
            "Gọi số 199",
            "Gọi cho Mẹ",
            "Đọc bộ nhớ tạm",
            "Đọc tin nhắn gần nhất",
            "Mở Facebook",
            "Mở Youtube",
            "Bật nhạc thư giãn",
            "Hôm nay ngày bao nhiêu ?",
            "Bây giờ là mấy giờ ?",
            "Thời tiết Hà Nội",
            "Tạo ghi chú",
            "Mở danh sách ghi chú",
            "Tạo nhắc nhở",
            "Đặt báo thức lúc 7 giờ 30",
            "Tìm Google Học viện Kỹ thuật Mật Mã",
            "Tìm Youtube Nơi này có anh",
            "5x5+2 bằng bao nhiêu ?",
            "Hệ điều hành Android là gì ?",
            "Obama là ai ?"
        )



    suggestArrayList= ArrayList()
        for( i in name.indices){
            val suggest= ModelSuggest(imgId[i],name[i])
            suggestArrayList.add(suggest)
        }
        suggestLv.isClickable=true
        suggestLv.adapter=ListSuggestAdapter(this,suggestArrayList)

        backToolbarSuggest.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
        }
    }
}