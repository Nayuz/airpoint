from PyQt5.QtWidgets import QWidget
from PyQt5.QtGui import QPainter, QPen, QColor
from PyQt5.QtCore import Qt, QPoint
import pyautogui
import time
import copy


class drawing_app(QWidget):
    slide_delay = 1.0
    last_slide_time = 0

    current_data = None
    prev_data = None

    scale_pos_x_start = 0
    scale_pos_y_start = 0
    scale_x = 1
    scale_y = 1

    def __init__(self, width, height):
        super().__init__()
        self.setWindowTitle('Transparent Drawing')
        self.setWindowFlags(Qt.Window | Qt.FramelessWindowHint | Qt.WindowStaysOnTopHint)
        self.setAttribute(Qt.WA_TranslucentBackground, True)
        self.setAttribute(Qt.WA_NoSystemBackground, True)
        self.showFullScreen()

        self.drawing_enabled = False
        self.pen_color = QColor(255, 0, 0)
        self.start_point = QPoint()
        self.lines = []
        self.pointer_position = QPoint()

        self.width = width
        self.height = height
        self.scale_x_end = width
        self.scale_y_end = height

    def toggle_drawing(self):
        self.drawing_enabled = not self.drawing_enabled
        print(f"Drawing mode {'ON' if self.drawing_enabled else 'OFF'}")

    def add_line(self, start, end):
        self.lines.append((start, end, self.pen_color))
        self.update()

    def set_pointer(self, position):
        self.pointer_position = position
        self.update()

    def paintEvent(self, event):
        painter = QPainter(self)
        painter.setRenderHint(QPainter.Antialiasing)

        # 배경을 투명하게 설정
        painter.fillRect(self.rect(), Qt.transparent)  # 배경을 투명하게 설정

        # 선 그리기
        for line in self.lines:
            start, end, color = line
            pen = QPen(color, 3, Qt.SolidLine)
            painter.setPen(pen)
            painter.drawLine(start, end)

        # 포인터 그리기
        if not self.pointer_position.isNull():
            pen = QPen(QColor(0, 255, 0), 5, Qt.SolidLine)
            painter.setPen(pen)
            painter.drawEllipse(self.pointer_position, 10, 10)



    def drawingapp_interaction(self, drawing_app, jsondata):
        mode = jsondata['mode']
        current_x = int((jsondata["pos"][0] - self.scale_pos_x_start) * drawing_app.width * self.scale_x)
        current_y = int((jsondata["pos"][1] - self.scale_pos_y_start) * drawing_app.height * self.scale_y)
        if self.prev_data != None:
            prev_x = int((self.prev_data["pos"][0] - self.scale_pos_x_start ) * drawing_app.width * self.scale_x)
            prev_y = int((self.prev_data["pos"][1] - self.scale_pos_y_start ) * drawing_app.height * self.scale_y)
            if mode =="draw":
                drawing_app.add_line(QPoint(prev_x, prev_y), QPoint(current_x, current_y))
            elif mode == "erase":
                if drawing_app.lines:  # 지울 선이 있는지 확인
                    eraser_size = 20  # 지우개 반경
                    new_lines = []

                    for line in drawing_app.lines:
                        start, end, color = line
                        x1, y1 = start.x(), start.y()
                        x2, y2 = end.x(), end.y()

                        # 직선과 점 사이의 최소 거리 계산
                        dx, dy = x2 - x1, y2 - y1
                        length_sq = dx ** 2 + dy ** 2

                        if length_sq == 0:  # 시작점과 끝점이 같은 경우
                            distance = ((current_x - x1) ** 2 + (current_y - y1) ** 2) ** 0.5
                        else:
                            t = max(0, min(1, ((current_x - x1) * dx + (current_y - y1) * dy) / length_sq))
                            proj_x = x1 + t * dx
                            proj_y = y1 + t * dy
                            distance = ((current_x - proj_x) ** 2 + (current_y - proj_y) ** 2) ** 0.5

                        # 지우개 크기보다 멀면 남기기
                        if distance > eraser_size:
                            new_lines.append(line)
                            drawing_app.lines = new_lines
                            drawing_app.update()
            elif mode == "slide":
                current_time = time.time()
                print(current_time - drawing_app.last_slide_time)
                if current_time - drawing_app.last_slide_time > drawing_app.slide_delay:
                    if prev_x is not None:
                        movement = (current_x - prev_x)* drawing_app.width
                        if movement < -50:
                            pyautogui.hotkey('right')
                            print("Slide Forward")
                        elif movement > 50:
                            pyautogui.hotkey('left')
                            print("Slide Backward")
                        drawing_app.last_slide_time = current_time
            
            elif mode == "set_scale":
                start_x1 = jsondata['pos'][0]
                start_y1 = jsondata['pos'][1]
                start_x2 = jsondata['pos2'][0]
                start_y2 = jsondata['pos2'][1]
                self.scale_pos_x_start = start_x1 if start_x1<start_x2 else start_x2
                self.scale_pos_y_start = start_y1 if start_y1<start_y2 else start_y2
                self.scale_x = 1 / abs(start_x1 - start_x2)
                self.scale_y = 1 / abs(start_y1 - start_y2)
                print([ self.scale_x, self.scale_y ])

            elif mode == "none":
                pass
        self.prev_data = copy.deepcopy(jsondata)
          