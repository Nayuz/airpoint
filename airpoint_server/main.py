import tcp_reciever
import draw
from PyQt5.QtWidgets import QApplication
from PyQt5.QtCore import QThread, pyqtSignal
import sys

class ServerThread(QThread):
    data_signal = pyqtSignal(dict)

    def __init__(self):
        super().__init__()
        self.receiver = tcp_reciever.DataReceiver()

    def run(self):
        self.receiver.data_received.connect(self.handle_data)
        self.receiver.start_server()  # 메인 루프를 실행하지 않음

    def handle_data(self, data):
        print(f"Received data: {data}")
        self.data_signal.emit(data)


def main():
    app = QApplication(sys.argv)
    screen = app.primaryScreen()
    size = screen.size()
    width, height = size.width(), size.height()

    drawing_app = draw.drawing_app(width, height)
    drawing_app.show()

    server_thread = ServerThread()
    server_thread.data_signal.connect(lambda data: drawing_app.drawingapp_interaction(drawing_app, data))
    server_thread.start()

    app.exec_()  # PyQt 메인 이벤트 루프 실행

if __name__ == "__main__":
    main()
