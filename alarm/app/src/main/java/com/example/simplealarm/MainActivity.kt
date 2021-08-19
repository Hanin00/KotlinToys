package com.example.simplealarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.simplealarm.databinding.ActivityMainBinding
import java.util.*

class MainActivity : AppCompatActivity() {
    companion object {
        //static 상수지정
        private const val M_SHARED_PREFERNECES_NAME = "time"
        private const val M_ALARM_KEY = "alarm"
        private const val M_ONOFF_KEY = "onOff"
        private val M_ALARM_REQUEST_CODE = 1000
    }

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        var view = binding.root
        setContentView(view)

        //뷰 초기화
        initOnOffBtn()
        ininChageAlarmBtn()

        //저장된 데이터 가져오기
        val model = fetchDateFromSharedPreferneces()

        renderView(model)
    }

    //모델 생성 및 sharedPreferences를 이용한 저장
    private fun saveAlarmModel(hour: Int, minute: Int, onOff: Boolean): DisplayModel {

        val model = DisplayModel(
            hour = hour,
            minute = minute,
            onOff = onOff
        )

        //time에 대한 db생성
        val sharedPreferences =
            getSharedPreferences(M_SHARED_PREFERNECES_NAME, Context.MODE_PRIVATE)

        //edit 모드로 열어서 작업(값 저장)
        with(sharedPreferences.edit()) {
            putString(M_ALARM_KEY, model.makeDataForDB())
            putBoolean(M_ONOFF_KEY, model.onOff)
            commit()
        }
        return model
    }

    private fun fetchDateFromSharedPreferneces(): DisplayModel {
        val sharedPreferences =
            getSharedPreferences(M_SHARED_PREFERNECES_NAME, Context.MODE_PRIVATE)

        //DB 에서 데이터 가져옴
        val timeDBValue = sharedPreferences.getString(M_ALARM_KEY, "09:30") ?: "09:30"
        val onOffValue = sharedPreferences.getBoolean(M_ONOFF_KEY, false)

        //시, 분 형식으로 가져온 데이터 스플릿
        val alarmData = timeDBValue.split(":")
        val alarmModel = DisplayModel(alarmData[0].toInt(), alarmData[1].toInt(), onOffValue)

        //보정 조정 예외처? - 브로드 캐스트 가져오기
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            M_ALARM_REQUEST_CODE,
            Intent(this, AlarmReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE
        ) // 있으면 가져오고 없으면 안만듦.(null)

        if ((pendingIntent == null) and alarmModel.onOff) {
            //알람은 꺼져있는데, 데이터 켜져있는 경우
            alarmModel.onOff = false
        } else if ((pendingIntent != null) and alarmModel.onOff.not()) {
            //알람은 켜져있는데 데이터는 꺼져있는 경우
            //알람을 취소함
            pendingIntent.cancel()
        }
        return alarmModel
    }


    //시간 재설정 버튼
    private fun ininChageAlarmBtn() {
        val changeAlarmBtn = binding.mainBtnReset
        changeAlarmBtn.setOnClickListener {
            val calendar = Calendar.getInstance()
            TimePickerDialog(this, { picker, hour, minute ->
                //데이터 저장
                val model = saveAlarmModel(hour, minute, false)
                //update view
                renderView(model)
                //기존 알람 삭제
                cancelAlarm()
                //24시간이 아닌 오전/오후 형식 사용하므로 get에서 false 줘야함.
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show()
        }
    }

    //알람 OnOff
    private fun initOnOffBtn() {
        val onOffBtn = binding.mainBtnSet
        onOffBtn.setOnClickListener {
            //저장된 데이터 확인
            val model = it.tag as? DisplayModel ?: return@setOnClickListener// 형변환 실패 시 null
            val newModel = saveAlarmModel(model.hour, model.minute, model.onOff.not()) // on Off 스위칭
            renderView(newModel)

            //on/off 분기처리
            if (newModel.onOff) {
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, newModel.hour)
                    set(Calendar.MINUTE, newModel.minute)

                    //이미 지나간 시간을 선택한 경우
                    if (before(Calendar.getInstance())) {
                        add(Calendar.DATE, 1) //하루 더하기
                    }
                }

                val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val intent = Intent(this, AlarmReceiver::class.java)
                val pendingIntent = PendingIntent.getBroadcast(
                    this, M_ALARM_REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT
                ) // 새로 만들어서 업데이트

                alarmManager.setInexactRepeating( //정확한 시간에 맞춰서 알람이 울림
                    AlarmManager.RTC_WAKEUP, // 실제 시간을 기준으로 wakeup
                    calendar.timeInMillis, //알림 발동할 시간
                    AlarmManager.INTERVAL_DAY, // 알림 주기. INTERVAL_DAY : 하루에 한 번 씩
                    pendingIntent
                )
            } else {
                //off 인 경우 알람을 제거해야함
                cancelAlarm()
            }
        }
    }

    //기존의 알람을 삭제함
    private fun cancelAlarm() {
        val pendingIntent = PendingIntent.getBroadcast(
            this, M_ALARM_REQUEST_CODE, intent, PendingIntent.FLAG_NO_CREATE
        ) //FLAG_NO_CREATE : 있으면 가져오고 없으면 안만듦(null)

        pendingIntent?.cancel() // 기존 알람 삭제
    }

    //최초 실행 또는 시간 재 설정시
    private fun renderView(model: DisplayModel) {

        binding.mainTextAmpm.apply { text = model.ampmText }
        binding.mainTextTime.apply {
            text = model.timeText
        }
        binding.mainBtnSet.apply {
            text = model.onOffText
            tag = model
        }
    }
}