import socket

# TCP 서버 설정
server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server_socket.bind(("0.0.0.0", 25565))  # 포트 25565 사용
server_socket.listen(1)
print("서버 대기 중...")

client_socket, addr = server_socket.accept()
print(f"{addr} 연결됨!")

# 데이터 수신 루프
while True:
    data = client_socket.recv(1024)
    if not data:
        print("연결 끊김")
        break
    print(f"받은 데이터: {data.decode('utf-8')}")
    client_socket.send("수신 확인".encode("utf-8"))
    
client_socket.close()
server_socket.close()
