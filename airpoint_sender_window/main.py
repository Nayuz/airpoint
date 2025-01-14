import cv2
import detect_hands as dh
import tkinter as tk
from tkinter import simpledialog, messagebox
import tcp_sender


def get_ip_and_port():
    """입력 창을 통해 IP 주소와 포트 번호를 입력받는 함수"""
    root = tk.Tk()
    root.withdraw()  # 메인 창을 숨김

    host = simpledialog.askstring("IP 입력", "데이터를 전송할 IP 주소를 입력하세요 (기본값: 127.0.0.1):")
    if not host:
        host = "127.0.0.1"

    port_input = simpledialog.askstring("포트 입력", "데이터를 전송할 포트 번호를 입력하세요 (기본값: 25565):")
    if not port_input:  # 비어 있으면 기본값
        port = 25565
    else:
        try:
            port = int(port_input)  # 숫자로 변환
        except ValueError:
            messagebox.showerror("입력 오류", "올바른 포트 번호를 입력하세요.")
            return None, None

    return host, port


def main():
    try:
        host, port = get_ip_and_port()
    except Exception as e:
        messagebox.showerror("입력 오류", f"IP 또는 포트 입력 중 오류가 발생했습니다: {e}")
        return

    # 웹캠 캡처
    cap = cv2.VideoCapture(0)
    if not cap.isOpened():
        print("Unable to open webcam.")
        return
    
    sender = tcp_sender.TCPClient()

    if sender.connect():
        hands = dh.mp_hands.Hands(static_image_mode=False, max_num_hands=2, min_detection_confidence=0.6)

        while cap.isOpened():
            ret, frame = cap.read()
            if not ret:
                print("Unable to read frame from webcam.")
                break

            frame = cv2.flip(frame, 1)
            rgb_frame = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
            results = hands.process(rgb_frame)

            jsondata = dh.twohand_landmarks_to_json(results)
            if jsondata:
                sender.send_data(jsondata)
            
            # 캠 화면을 띄우는 부분
            cv2.imshow("Webcam", frame)

            if cv2.waitKey(1) & 0xFF == ord('q'):
                break

        cap.release()
        cv2.destroyAllWindows()
        sender.close()  # 연결 종료
    else:
        return

if __name__ == "__main__":
    main()
