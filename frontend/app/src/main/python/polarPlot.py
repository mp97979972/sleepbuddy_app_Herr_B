import numpy as np
import cv2
import matplotlib.pyplot as plt
from PIL import Image
import io
import base64

def main(R, Theta, SQ):

    r = R
    theta = Theta
    sQ = SQ

    rmax = int(r[-1])+1

    rticks = []
    for zahl in range(1,rmax+1):
        rticks.append(zahl)

    plt.rcParams['font.size'] = 12
    fig, ax = plt.subplots(subplot_kw={'projection': 'polar'})
    #ax.scatter(theta, r, c='k', s=1, cmap='hsv', alpha=0.75)#color(c) is not correct
    c = ax.scatter(theta, r, c=sQ, s=1, cmap='jet')#color(c) is not correct
    ax.set_rmax(rmax)
    ax.set_rmin(0)
    ax.set_rticks(rticks)  # Less radial ticks, one per hour

    ax.set_rlabel_position(-157.5)  # Move radial labels away from plotted line
    ax.set_xticklabels(['    rechts', '               Rücken/rechts', 'Rücken', 'Rücken/links             ', 'links  ', 'Bauch/links           ', 'Bauch', '              Bauch/rechts'])
    ax.margins(0) #let the plot start at zero

    #the following code is for the connection with the image view
    fig.canvas.draw() #use this canvas to convert data to numpy array
    img = np.fromstring(fig.canvas.tostring_rgb(),dtype=np.uint8,sep='')
    img = img.reshape(fig.canvas.get_width_height()[::-1]+(3,)) #reshape data
    img = np.delete(img,slice(580,640),1) #delete last 40 columns
    img = np.delete(img,slice(80),1) #delete first 80 columns
    img = cv2.cvtColor(img,cv2.COLOR_RGB2BGR)
    #now its converted to cv2 image..pyplot
    pil_im = Image.fromarray(img)
    buff = io.BytesIO()
    pil_im.save(buff,format="PNG")
    img_str = base64.b64encode(buff.getvalue())

    return ""+str(img_str,'utf-8')
