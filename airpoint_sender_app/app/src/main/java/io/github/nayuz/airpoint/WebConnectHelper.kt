package io.github.nayuz.airpoint

import android.util.Log
import okhttp3.*

val client = OkHttpClient()
class WebConnectHelper {

    private val client = OkHttpClient()
    private var webSocket: WebSocket? = null

    fun connectWebSocket(url: String) {
        val request = Request.Builder().url(url).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("WebSocket", "연결 성공!")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("WebSocket", "서버 메시지: $text")
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("WebSocket", "연결 닫힘: $reason")
                webSocket.close(1000, null)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("WebSocket", "연결 실패: ${t.message}")
            }
        })
    }

    fun sendMessage(message: String) {
        webSocket?.send(message) ?: Log.e("WebSocket", "WebSocket 연결이 설정되지 않았습니다.")
    }

    fun closeConnection() {
        webSocket?.close(1000, "연결 종료")
        webSocket = null
    }
}
