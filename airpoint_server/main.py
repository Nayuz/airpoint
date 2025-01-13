import socket
import json
import motionDetect
import copy

def receive_data(client_socket):
    BUFFER_SIZE = 1024
    data = b""

    while True:
        # 1. 길이 정보(10바이트 고정) 수신
        length_data = client_socket.recv(10)
        if not length_data:
            break  # 연결이 끊어졌다면 종료

        data_length = int(length_data.decode('utf-8').strip())

        # 2. 해당 길이만큼의 데이터만 수신
        packet = client_socket.recv(data_length)
        if not packet:
            break  # 연결이 끊어졌다면 종료

        # 받은 데이터를 바로 처리
        data += packet
        data = json.loads(data)
        # 예시: 데이터 처리 완료 후 해당 데이터를 바로 반환하려면 다음과 같이 할 수 있음
        return data

    # 데이터 반환
    return data


# TCP 서버 설정
server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server_socket.bind(("0.0.0.0", 25565))  # 포트 25565 사용
server_socket.listen(1)
print("서버 대기 중...")

client_socket, addr = server_socket.accept()
print(f"{addr} 연결됨!")

prevData = None
currentData = None

delay = 0
# 데이터 수신 루프
while True:
    action = None
    data = receive_data(client_socket)
    print(data)
    if not data:
        print("연결 끊김")
        break
    prevData = copy.deepcopy(currentData)
    print("이전 데이터 : ", prevData)
    ## 데이터 인스턴스가 없으면 새로 만들어주고 있다면 업데이트
    currentData = motionDetect.motionData(data)
    ##이전 데이터가 있는 경우에만 비교
    if prevData:
        action = currentData.motionDetect(prevData)
    if action is not None and not delay:
        print(f"동작 : {action}")
        delay = 1000

    currentData = motionDetect.motionData
    client_socket.send("수신 확인".encode("utf-8"))
    if delay > 0:
        delay -= 1
    
client_socket.close()
server_socket.close()
