# airpoint

## 카메라 또는 휴대폰으로 손을 인식해서 화면을 강조하는 프로그램

airpoint는 공중에 점을 찍어 표시하겠다는 의미를 가진 프로그램입니다.

## 설치, 설정

git을 통한 설치가 안 되는 경우

https://drive.google.com/drive/folders/13W_9vGCIDhRIHI8c9FsVUODdhiC8bLPX?usp=sharing

해당 링크에서 실행 파일을 받을 수 있습니다.

사용자는 선을 그리거나 좌우키를 조작할 PC에
exe_files 내부의 airpoint_server.exe 설치하고,

카메라가 설치된 PC에는
exe_files 내부의 airpoint_sender.exe를 설치합니다.

또는, 카메라로 사용할 안드로이드 기기에
exe_files 내부의 app-debug.apk를 설치합니다.
이 apk는 설치하면 AirPoint라는 앱이 됩니다.

airpoint_server.exe를 실행한 뒤에 포트를 설정하고,
airpoint_sender.exe 또는 AirPoint 앱을 실행합니다.

airpoint_sender.exe 또는 AirPoint 앱에
airpoint_server.exe가 설치된 PC의 IP 주소와 지정한 포트 번호를 설정합니다.

airpoint_sender와 airpoint_server가 같은 컴퓨터에 설치되어 사용될 경우,
airpoint_sender.exe의 설정은 포트만 맞춰주면 됩니다.
전부 기본값일 경우 값을 입력하지 않고 엔터만 쳐도 됩니다.

## 종료 방법
airpoint_sender는 캠 화면을 띄운 뒤 q 키를 누르면 종료됩니다.

airpoint_server는 아무 창에서나 Ctrl+Q를 누르면 종료됩니다.

## 조작

### 왼손으로 조작 모드를 선택합니다.
왼손으로는 엄지만 편 상태일 때 가상의 테두리를 설정합니다.
그 때의 왼손 엄지 끝과 오른손 검지 끝을 기준으로 동작을 창 전체에 옮깁니다.

왼손 검지만 폈을 때는 포인터를 따라 선을 그릴 수 있습니다.

왼손 검지, 중지만 폈을 때는 포인터를 따라 선을 지울 수 있습니다.

왼손 검지, 중지, 약지만 폈을 때는 좌우키를 조작할 수 있습니다.

오른쪽 손을 왼쪽으로 쓸면 오른쪽 방향으로 움직입니다.

오른쪽 손을 오른쪽으로 쓸면 왼쪽 방향으로 움직입니다.

스마트폰의 화면 조작을 생각하면 편합니다.

### 오른손 검지로 포인터를 움직일 수 있습니다.

손바닥이 보이는 각도일 때 다른 때에 비해서 인식이 잘 됩니다.
