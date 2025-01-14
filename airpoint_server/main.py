import tcp_reciever
import draw
from PyQt5.QtWidgets import QApplication, QMainWindow, QInputDialog
from PyQt5.QtCore import QThread, pyqtSignal, QEvent, Qt
from PyQt5.QtGui import QPainter, QPen, QFont
import sys
import keyboard

class ServerThread(QThread):
    data_signal = pyqtSignal(dict)

    def __init__(self, port):
        super().__init__()
        self.port = port
        self.receiver = tcp_reciever.DataReceiver()

    def run(self):
        self.receiver.data_received.connect(self.handle_data)
        self.receiver.start_server(port = self.port)  # 메인 루프를 실행하지 않음

    def handle_data(self, data):
        print(f"Received data: {data}")
        self.data_signal.emit(data)

class MainWindow(QMainWindow):
    def __init__(self):
        super().__init__()
        self.setWindowTitle("AirPoint")
        self.setFixedSize(400, 100)

    def paintEvent(self, event):
        painter = QPainter(self)
        painter.setRenderHint(QPainter.Antialiasing)

        # 텍스트 그리기
        painter.setPen(QPen(Qt.black))  # 검정색 펜 사용
        painter.setFont(QFont('Arial', 20))  # 폰트 크기 20으로 설정
        painter.drawText(50, 50, "Ctrl+Q를 눌러 종료합니다")  # 텍스트 그리기


    def eventFilter(self, obj, event):
        if event.type() == QEvent.KeyPress:
            # Ctrl+Q를 눌렀을 때 프로그램 종료
            if event.key() == Qt.Key_Q and event.modifiers() == Qt.ControlModifier:
                print("Ctrl+Q를 눌러서 프로그램을 종료합니다.")
                QApplication.quit()  # 프로그램 종료
                return True  # 이벤트 처리 완료
        return super().eventFilter(obj, event)  # 이벤트 필터가 처리하지 않은 경우, 기본 동작 처리

def main():
    app = QApplication(sys.argv)


     # 포트 번호 입력 창 띄우기
    port, ok = QInputDialog.getInt(None, "포트 번호 입력", "포트 번호를 입력하세요:", 25565, 1, 65535, 1)
    if not ok:
        print("포트 번호를 입력하지 않았습니다. 프로그램을 종료합니다.")
        sys.exit(1)

    screen = app.primaryScreen()
    size = screen.size()
    width, height = size.width(), size.height()

    drawing_app = draw.drawing_app(width, height)
    drawing_app.show()

    server_thread = ServerThread(port)
    server_thread.data_signal.connect(lambda data: drawing_app.drawingapp_interaction(drawing_app, data))
    server_thread.start()

    main_window = MainWindow()
    app.installEventFilter(main_window)  # 애플리케이션 전체에서 이벤트 필터 설치
    main_window.show()

    # Ctrl+Q 전역 핫키 감지 (비활성화된 창에서도 작동)
    keyboard.add_hotkey('ctrl+q', lambda: QApplication.quit())

    app.exec_()  # PyQt 메인 이벤트 루프 실행

if __name__ == "__main__":
    main()

