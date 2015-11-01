#!/usr/bin/env python
# -*- coding: utf-8 -*-
import cv2
import time
import numpy as np
import itertools as it

class Region:
  def __init__(self, label):
    self.pixels = []
    self.label = label
    self.average = 0
  def area(self):
    return len(self.pixels)
  def merge(self, other):
    self.pixels &= other.pixels
  def boundaries():
    pass

def regionGrowing(src,thresh = 10, minarea = 20):
  print thresh, minarea
  # ラベリングマップ
  h, w = src.shape[:2]
  labels = [[-1 for x in range(0,w)] for y in range(0,h)]
  # ラベルの面積
  infos = {}
  # 現在のラベルのインデックス
  labelCnt = 0
  for y in range(0,h):
    for x in range(0,w):
      # すでにラベリング済みピクセルなら飛ばす
      if labels[y][x] is not -1:
        continue
      stack = [(x,y,0,0)]
      pivot = src[y][x]
      # ラベルを構成する境界画素集合, 画素集合
      pixels = set()
      # 平均輝度
      vsum = 0
      # 深さ優先探索で拡張させる
      while len(stack) is not 0:
        xx, yy, dx, dy = stack.pop()
        # 領域外なら飛ばす
        if xx < 0 or yy < 0 or w <= xx or h <= yy:
          continue
        vv = src[yy][xx]
        dist = vv > pivot and vv-pivot or pivot-vv
        if labels[yy][xx] is -1 and dist < thresh:
          vsum += vv
          pixels.add((xx,yy))
          labels[yy][xx] = labelCnt
          for j in range(-1,2):
            for i in range(-1,2):
              if not (j is 0 and i is 0):
                stack.append((xx+i,yy+j,i,j))
      infos[labelCnt] = {
        "area": len(pixels),
        "average": vsum//len(pixels),
        "pixels": pixels,
        "index": labelCnt
      }
      labelCnt += 1
  print labelCnt
  # すべての領域の面積がmenarea以上になるように繰り返す
  flag = True
  while flag:
    flag = False
    for l in infos.keys():
      if infos[l]["area"] >= minarea:
        continue
      flag = True
      # 隣接するラベルを探す
      neis = set()
      for x,y in infos[l]["pixels"]:
        for yy in range(y-1,y+2):
          for xx in range(x-1,x+2):
            if xx < 0 or yy < 0 or w <= xx or h <= yy or (xx is x and yy is y):
              continue
            ll = labels[yy][xx]
            if ll != l:
              neis.add(ll)
      # 隣接する領域の中から最も勾配が小さい領域に塗り替える
      minl = neis.pop()
      mind = infos[l]["average"] - infos[minl]["average"]
      while len(neis) > 0:
        _minl = neis.pop()
        _mind = infos[_minl]["average"] - infos[minl]["average"]
        if _mind < mind:
          mind, minl = _mind, _minl
      # 面積を増やす
      infos[minl]["pixels"] |= infos[l]["pixels"]
      infos[minl]["area"]  = len(infos[minl]["pixels"])
      # ラベルを塗り替える
      for x,y in infos[l]["pixels"]:
        labels[y][x] = minl
      del infos[l]
      labelCnt -= 1
    assert len(infos) == labelCnt
    for l in infos.keys():
      assert infos[l]["area"] >= minarea
    if len(infos) != labelCnt:
      print "len(infos): {0}, labelCnt: {1}".format(len(infos),labelCnt)
      raise AssertionError
  return labels, infos
def drawBoundaries(src,labels):
  h,w = src.shape[:2]
  # bounds = [[0 for i in range(0,w)] for j in range(0,h)]
  for y in range(0,h):
    for x in range(0,w):
      label = labels[y][x]
      assert label != -1
      for yy in range(y-1,y+2):
        for xx in range(x-1,x+2):
          if xx < 0 or yy < 0 or w <= xx or h <= yy:
            continue
          if labels[yy][xx] != label:
            src[y][x] = np.array([255,255,255])
  return src

if __name__ == "__main__":
  print cv2.__version__
  src = cv2.imread("../res/lion.png")
  h, w = src.shape[:2]
  size = 256
  if w > h:
    src = cv2.resize(src,(size,int(size*h/w)))
  else:
    src = cv2.resize(src,(int(size*w/h),size))
  def imshow(img, name, i):
    cv2.namedWindow(name)
    cv2.moveWindow(name,img.shape[1]*i,0)
    cv2.imshow(name,img)
  # Mean-Shift
  ms = cv2.pyrMeanShiftFiltering(src,30,20)
  imshow(ms, "Mean-Shift", 0)
  # Gray
  gray = cv2.cvtColor(ms,cv2.COLOR_RGB2GRAY)
  imshow(gray,"Gray", 1)
  # Sobel
  sobelX = cv2.Sobel(gray,cv2.CV_32F,1,0)
  sobelY = cv2.Sobel(gray,cv2.CV_32F,0,1)
  mag, ang = cv2.cartToPolar(sobelX,sobelY)
  # 正規化してuint8画像にする
  minv,maxv,minl,maxl = cv2.minMaxLoc(mag)
  mag  = ((mag-minv) / (maxv-minv))*255
  img_sobel = np.uint8(mag)
  imshow(img_sobel, "Sobel", 2)
  # Otsu Binary
  thresh, obin = cv2.threshold(img_sobel,0, 255, cv2.THRESH_BINARY|cv2.THRESH_OTSU)
  imshow(obin, "Binary-Otsu", 3)
  # Mean-Shift Boundaries
  labels, infos = regionGrowing(gray,20,40)
  bounds = drawBoundaries(src,labels)
  imshow(bounds, "regionGrowing", 4)
  cv2.waitKey()
