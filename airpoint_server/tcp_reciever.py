import socket
import json
import copy
import motionDetect
from PyQt5.QtCore import pyqtSignal, QObject

class DataReceiver(QObject):
    data_received = pyqtSignal(dict)  # 데이터를 보내는 시그널

    def start_server(self, host='0.0.0.0', port=25565):
        server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        server_socket.bind((host, port))
        server_socket.listen(1)
        client_socket, addr = server_socket.accept()

        while True:
            data = receive_data(client_socket)
            if data:
                self.data_received.emit(data)  # 데이터를 전달


def receive_data(client_socket):
    BUFFER_SIZE = 1024  # 버퍼 크기
    data = b""  # 받은 데이터를 저장할 변수

    # 먼저 길이 정보(10바이트 고정) 수신
    length_data = client_socket.recv(10)
    if not length_data:
        return None  # 길이 정보가 없으면 종료

    # 길이 정보가 올바른지 확인
    data_length = int(length_data.decode('utf-8').strip())

    # 데이터를 받기 위한 루프
    while len(data) < data_length:
        packet = client_socket.recv(min(BUFFER_SIZE, data_length - len(data)))  # 남은 길이만큼 받기
        if not packet:
            break  # 데이터가 없으면 종료
        data += packet  # 받은 데이터를 계속 추가

    if len(data) == data_length:
        return json.loads(data)  # JSON 파싱하여 반환
    return None  # 데이터가 올바르게 수신되지 않으면 None 반환

def start_server(host='0.0.0.0', port=25565):
    """서버 시작 함수"""
    server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server_socket.bind((host, port))
    server_socket.listen(1)
    print("서버 대기 중...")

    client_socket, addr = server_socket.accept()
    print(f"{addr} 연결됨!")

    prevData = None
    currentData = None

    delay = 0
    while True:
        data = receive_data(client_socket)
        if not data:
            print("연결 끊김")
            break

        print(f"수신된 데이터: {data}")

        prevData = copy.deepcopy(currentData)
        currentData = motionDetect.motionData(data)

        # action 변수를 초기화
        action = None  # action을 None으로 초기화하여 조건문에서 오류를 방지

        if prevData is not None:
            action = currentData.motionDetect(prevData)

        if action is not None and not delay:
            
            print(f"동작: {action}")
            delay = 100

        if delay > 0:
            delay -= 1

    client_socket.close()
    server_socket.close()
