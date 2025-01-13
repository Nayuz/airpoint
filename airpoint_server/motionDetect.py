class motionData:
    def __init__(self, data):
        self.mode = 0
        self.pos = None 
        self.update(data)
    
    def update(self, data):
        print(f"데이터 처리 : {data}")
        self.mode = data['mode']
        self.pos = data['pos']
    

    def motionDetect(self, prevData):
        if (prevData is not None):
            print(prevData.mode)
            print(self.mode)
            if (prevData.mode == 3 and self.mode == 3):
                change_x = self.pos[0] - prevData.pos[0]
                change_y = self.pos[1] - prevData.pos[1]
                #가로 변화량이 더 많다면
                if (change_x / change_y)**2 > 1:
                    if change_x > 0:
                        return "right"
                    elif change_x < 0:
                        return "left"
                    else:
                        return None
        return None


