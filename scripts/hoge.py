import cv2
import numpy as np

if __name__ == "__main__":
    n = np.zeros((30,255,3),np.uint8)
    for y in range(0,30):
        for x in range(0,180):
            n[y][x] = [x,255,255]
    n = cv2.cvtColor(n,cv2.COLOR_HSV2BGR)
    cv2.imshow("hoge",n)
    cv2.waitKey()
