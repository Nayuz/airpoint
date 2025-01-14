import socket

class TCPClient:
    def __init__(self, host='127.0.0.1', port=25565):
        """
        TCP 클라이언트 초기화, 서버와의 연결을 관리하는 클래스
        """
        self.host = host
        self.port = port
        self.client_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

    def connect(self):
        """서버와 연결"""
        try:
            self.client_socket.connect((self.host, self.port))
            print(f"서버 {self.host}:{self.port}에 연결됨!")
        except Exception as e:
            print(f"연결 오류: {e}")
            return False
        return True

    def send_data(self, data_to_send):
        """
        데이터를 서버에 전송하는 함수
        Parameters:
            data_to_send (str): 서버에 전송할 데이터
        """
        # 데이터 길이를 10바이트로 맞춰서 전송
        data_length = len(data_to_send)
        self.client_socket.sendall(f"{data_length:10}".encode())  # 길이 정보 전송 (10자리로 맞춤)

        # 실제 데이터 전송
        self.client_socket.sendall(data_to_send.encode())
        print(f"전송한 데이터: {data_to_send}")

        # 서버의 응답 받기
        # response = self.client_socket.recv(1024)
        # print("서버의 응답:", response.decode())

    def close(self):
        """서버와의 연결을 종료"""
        self.client_socket.close()
        print("서버와의 연결을 종료합니다.")
