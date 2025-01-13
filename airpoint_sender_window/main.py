import cv2
import json
import detect_hands as dh
import tcp_sender

def main():
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
