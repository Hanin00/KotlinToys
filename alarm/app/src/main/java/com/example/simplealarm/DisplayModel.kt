package com.example.simplealarm

data class DisplayModel(
    val hour: Int,
    val minute: Int,
    var onOff: Boolean
) {
    fun makeDataForDB(): String {
        return "$hour:$minute"
    }

    //형식에 맞게 시:분 가져오기
    val timeText: String
        get() {
            val h = "%02d".format(if (hour < 12) hour else hour - 12)
            val m = "%02d".format(minute)

            return "$h:$m"
        }

    val ampmText: String
        get() {
            return if (hour < 12) "AM" else "PM"
        }

    val onOffText: String
        get() {
            return if (onOff) "알람 꺼져있음" else "알람 설정되어 있음"
        }

}