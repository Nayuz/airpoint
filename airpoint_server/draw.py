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
    
    # 추가된 속성들: 보정된 좌표 및 이전 좌표
    smoothed_x = None
    smoothed_y = None
    prev_smoothed_x = None
    prev_smoothed_y = None
    

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
        self.last_slide_time = time.time()


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
            print(f"Drawing pointer at: {self.pointer_position}")  # 디버깅: 포인터 그리기 확인
            pen = QPen(QColor(0, 255, 0), 5, Qt.SolidLine)
            painter.setPen(pen)
            painter.drawEllipse(self.pointer_position, 10, 10)



    def drawingapp_interaction(self, jsondata):
        mode = jsondata['mode']
        current_x = int((jsondata["pos"][0] - self.scale_pos_x_start) * self.width * self.scale_x)
        current_y = int((jsondata["pos"][1] - self.scale_pos_y_start) * self.height * self.scale_y)


        # 포인터 위치 업데이트 (모드와 상관없이 항상 적용)
        pointer_x = int((jsondata["pos"][0] - self.scale_pos_x_start) * self.width * self.scale_x)
        pointer_y = int((jsondata["pos"][1] - self.scale_pos_y_start) * self.height * self.scale_y)

        if 0 <= pointer_x <= self.width and 0 <= pointer_y <= self.height:
            self.set_pointer(QPoint(pointer_x, pointer_y))
        else:
            print("Pointer out of bounds.")

        # 보정된 좌표 초기화
        if self.smoothed_x is None or self.smoothed_y is None:
            self.smoothed_x = current_x
            self.smoothed_y = current_y
            self.prev_smoothed_x = current_x
            self.prev_smoothed_y = current_y

        if mode == "set_scale":
            start_x1 = jsondata['pos'][0]
            start_y1 = jsondata['pos'][1]
            start_x2 = jsondata['pos2'][0]
            start_y2 = jsondata['pos2'][1]

            self.scale_pos_x_start = min(start_x1, start_x2)
            self.scale_pos_y_start = min(start_y1, start_y2)
            self.scale_x = 1 / abs(start_x1 - start_x2)
            self.scale_y = 1 / abs(start_y1 - start_y2)
            print([self.scale_x, self.scale_y])
        
        elif mode == "draw":
            smoothing_factor = 0.2  # 필터 강도

            # 저역 통과 필터 적용
            self.smoothed_x = smoothing_factor * current_x + (1 - smoothing_factor) * self.smoothed_x
            self.smoothed_y = smoothing_factor * current_y + (1 - smoothing_factor) * self.smoothed_y

            # 이전 보정된 좌표와 현재 보정된 좌표를 사용하여 선을 그림
            self.add_line(
                QPoint(int(self.prev_smoothed_x), int(self.prev_smoothed_y)),
                QPoint(int(self.smoothed_x), int(self.smoothed_y))
            )

            # 이전 보정된 좌표 업데이트
            self.prev_smoothed_x = self.smoothed_x
            self.prev_smoothed_y = self.smoothed_y

        elif mode == "erase":
            if self.lines:  # 지울 선이 있는지 확인
                eraser_size = 20  # 지우개 반경
                new_lines = []

                for line in self.lines:
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

                self.lines = new_lines
                self.update()

        elif mode == "slide":
            current_time = time.time()
            print(f"Time since last slide action: {current_time - self.last_slide_time}")

            if current_time - self.last_slide_time > self.slide_delay:
                if self.prev_data is not None:
                    print(f"Prev data: {self.prev_data['pos'][0]}")
                    prev_x = int((self.prev_data["pos"][0] - self.scale_pos_x_start) * self.width * self.scale_x)
                    prev_y = int((self.prev_data["pos"][1] - self.scale_pos_y_start) * self.height * self.scale_y)

                    movement = current_x - prev_x
                    print(f"Calculated movement: {movement}")

                    if movement < -30:  # 오른쪽 슬라이드
                        pyautogui.hotkey('right')
                        print("Slide Forward")

                        # 슬라이드 동작 이후 선 삭제
                        self.lines.clear()
                        self.update()
                        self.last_slide_time = current_time

                    elif movement > 30:  # 왼쪽 슬라이드
                        pyautogui.hotkey('left')
                        print("Slide Backward")

                        # 슬라이드 동작 이후 선 삭제
                        self.lines.clear()
                        self.update()
                        self.last_slide_time = current_time

                    else:
                        print("No slide action detected.")

        elif mode == "none":
            # none 모드에서 보정된 좌표와 이전 좌표 초기화
            print("Resetting smoothed and previous coordinates.")
            self.smoothed_x = None
            self.smoothed_y = None
            self.prev_smoothed_x = None
            self.prev_smoothed_y = None
            
            
        # 이전 데이터를 현재 데이터로 업데이트
        self.prev_data = copy.deepcopy(jsondata)
