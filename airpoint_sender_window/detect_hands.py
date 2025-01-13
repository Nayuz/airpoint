import mediapipe as mp
import math
import json

# Mediapipe Hands 초기화
mp_hands = mp.solutions.hands




def detect_left_hand_mode(landmarks):
    """손 모드 인식 (여기서는 손가락을 기준으로 'draw', 'erase', 'slide' 모드를 결정)"""
    thumb_tip = landmarks[mp_hands.HandLandmark.THUMB_TIP]
    index_tip = landmarks[mp_hands.HandLandmark.INDEX_FINGER_TIP]
    middle_tip = landmarks[mp_hands.HandLandmark.MIDDLE_FINGER_TIP]
    ring_tip = landmarks[mp_hands.HandLandmark.RING_FINGER_TIP]
    pinky_tip = landmarks[mp_hands.HandLandmark.PINKY_TIP]
    wrist = landmarks[mp_hands.HandLandmark.WRIST]

    def landmark_distance(p1, p2):
        return math.sqrt((p1.x - p2.x)**2 + (p1.y - p2.y)**2)

    def is_finger_open(tip, mcp):
        return 1 if landmark_distance(tip, wrist) - landmark_distance(mcp, wrist) > 0 else 0

    fingers_open = [
        is_finger_open(index_tip, landmarks[mp_hands.HandLandmark.INDEX_FINGER_MCP]),
        is_finger_open(middle_tip, landmarks[mp_hands.HandLandmark.MIDDLE_FINGER_MCP]),
        is_finger_open(ring_tip, landmarks[mp_hands.HandLandmark.RING_FINGER_MCP]),
        is_finger_open(pinky_tip, landmarks[mp_hands.HandLandmark.PINKY_MCP])
    ]

    if fingers_open[0] and not any(fingers_open[1:]):
        return "draw"
    elif fingers_open[0] and fingers_open[1] and not any(fingers_open[2:]):
        return "erase"
    elif fingers_open[0] and fingers_open[1] and fingers_open[2] and not fingers_open[3]:
        return "slide"
    elif all(fingers_open):
        return "none"
    elif not any(fingers_open):
        return "none"
    else:
        return "none"

def process_right_hand(hand_landmarks):
    """오른손 좌표를 받아서 JSON으로 변환"""
    index_finger_tip = hand_landmarks.landmark[mp_hands.HandLandmark.INDEX_FINGER_TIP]
    return [index_finger_tip.x, index_finger_tip.y]


def twohand_landmarks_to_json(results):
    if results.multi_hand_landmarks and len(results.multi_hand_landmarks) == 2:
        leftHand = None
        rightHand = None
        if (results.multi_handedness[0] != results.multi_handedness[1]):
            if results.multi_handedness[0].classification[0].label == "Left":
                leftHand = results.multi_hand_landmarks[0]
                rightHand = results.multi_hand_landmarks[1]
            else :
                rightHand = results.multi_hand_landmarks[0]
                leftHand = results.multi_hand_landmarks[1]
            mode = detect_left_hand_mode(leftHand.landmark)
            pos = process_right_hand(rightHand)
            json_data = {"mode":mode, "pos":pos}
            json_data = json.dumps(json_data)
            print(json_data)
            return json_data
    return