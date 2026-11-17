import sys
from PIL import Image, ImageFilter
import numpy as np
from collections import deque
def cut(path, frac=0.58):
    im=Image.open(path).convert('RGB'); w,h=im.size
    im=im.crop((0,0,w,int(h*frac)))
    a=np.asarray(im).astype(int); H,W,_=a.shape
    border=np.concatenate([a[0],a[-1],a[:,0],a[:,-1]])
    bg=np.median(border,axis=0)
    d=np.abs(a-bg).sum(axis=2)
    fg=d>70
    # keep component(s) of fg connected to center area, largest
    from scipy import ndimage
    lab,nl=ndimage.label(ndimage.binary_closing(fg,iterations=2))
    sizes=ndimage.sum(fg,lab,range(1,nl+1))
    keep=np.zeros_like(fg)
    for i,s in enumerate(sizes,1):
        if s>0.15*max(sizes): keep|=(lab==i)
    keep=ndimage.binary_fill_holes(keep)
    keep=ndimage.binary_erosion(keep,iterations=1)
    alpha=Image.fromarray((keep*255).astype('uint8')).filter(ImageFilter.GaussianBlur(0.8))
    out=im.convert('RGBA'); out.putalpha(alpha); return out
if __name__=='__main__':
    cut(sys.argv[1]).save(sys.argv[2])
