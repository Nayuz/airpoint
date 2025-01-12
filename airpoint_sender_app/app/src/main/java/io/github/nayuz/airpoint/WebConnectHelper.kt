package io.github.nayuz.airpoint

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket

class WebConnectHelper {

    private var socket: Socket? = null
    private var outputStream: DataOutputStream? = null
    private var inputStream: DataInputStream? = null

    // TCP 연결 설정
    suspend fun connectTcpServer(address: String, port: Int) {
        try {
            withContext(Dispatchers.IO) {
                socket = Socket(address, port)  // 서버에 연결
                outputStream = DataOutputStream(socket?.getOutputStream())
                inputStream = DataInputStream(socket?.getInputStream())
                Log.d("WebConnectHelper", "TCP 서버 연결 성공: $address:$port")
            }
        } catch (e: Exception) {
            Log.e("WebConnectHelper", "TCP 서버 연결 실패: ${e.message}")
        }
    }

    // 데이터 전송
    suspend fun sendData(data: String) {
        try {
            withContext(Dispatchers.IO) {
                val jsonBytes = data.toByteArray(Charsets.UTF_8)
                val formattedLength = "%10d".format(jsonBytes.size)  // 노란 줄 제거된 방식
                outputStream?.write(formattedLength.toByteArray(Charsets.UTF_8))
                outputStream?.write(jsonBytes)
                outputStream?.flush()
                Log.d("WebConnectHelper", "데이터 전송: $data")
            }
        } catch (e: Exception) {
            Log.e("WebConnectHelper", "데이터 전송 실패: ${e.message}")
        }
    }


    // 연결 종료
    fun closeConnection() {
        try {
            socket?.close()
            outputStream?.close()
            inputStream?.close()
            Log.d("WebConnectHelper", "TCP 연결 종료")
        } catch (e: Exception) {
            Log.e("WebConnectHelper", "연결 종료 중 오류: ${e.message}")
        }
    }
}
