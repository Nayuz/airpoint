package io.github.nayuz.airpoint

import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket

class TcpConnectHelper (private val activity: AppCompatActivity,
                        private val tcpAddressEditText: EditText,
                        private val connectTcpButton: Button
){
    private var socket: Socket? = null
    private var outputStream: DataOutputStream? = null
    private var inputStream: DataInputStream? = null

    fun init(){
        connectTcpButton.setOnClickListener {
            val address = tcpAddressEditText.text.toString().split(":")
            if (address.size == 2) {
                val ip = address[0]
                val port = address[1].toInt()

                CoroutineScope(Dispatchers.Main).launch {
                    connectTcpServer(ip, port)
                    Toast.makeText(activity, "TCP 서버 연결 시도 중: $ip:$port", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(activity, "올바른 TCP 주소를 입력하세요 (예: 192.168.0.2:8080)", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // TCP 연결 설정
    suspend fun connectTcpServer(address: String, port: Int) {
        try {
            withContext(Dispatchers.IO) {
                socket = Socket(address, port)  // 서버에 연결
                outputStream = DataOutputStream(socket?.getOutputStream())
                inputStream = DataInputStream(socket?.getInputStream())
                Log.d("TcpConnectHelper", "TCP 서버 연결 성공: $address:$port")
            }
        } catch (e: Exception) {
            Log.e("TcpConnectHelper", "TCP 서버 연결 실패: ${e.message}")
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
                Log.d("TcpConnectHelper", "데이터 전송: $data")
            }
        } catch (e: Exception) {
            Log.e("TcpConnectHelper", "데이터 전송 실패: ${e.message}")
        }
    }


    // 연결 종료
    fun closeConnection() {
        try {
            socket?.close()
            outputStream?.close()
            inputStream?.close()
            Log.d("TcpConnectHelper", "TCP 연결 종료")
        } catch (e: Exception) {
            Log.e("TcpConnectHelper", "연결 종료 중 오류: ${e.message}")
        }
    }
}
