from PIL import Image, ImageDraw, ImageFont
import os
import sys
import argparse
import json
import math
import numpy as np
import segments
# from gradients import interpolationGradient
import seaborn as sns; sns.set()
from matplotlib import pyplot as plt

# G = interpolationGradient()
# G.add([235, 64, 52], 0.)
# G.add([235, 140, 52], 0.33)
# G.add([235, 223, 52], 0.67)
# G.add([162, 235, 52], 1.)
# G.sort()

DEBUG = False

resolution = 0.01
dpb = 500 # Dots per block; each image should be (dpb x dpb)

# def red_sigmoid(x):
#     return 1. / (1. + math.exp(10*x-5))

# v_red_sigmoid = np.vectorize(red_sigmoid)

# def blu_sigmoid(x):
#     return 1. / (1. + math.exp(5-10*x))

# v_blu_sigmoid = np.vectorize(blu_sigmoid)

# def gre_bell(x):
#     return 0.5*math.exp(-10*((x-0.5)**2))

# v_gre_bell = np.vectorize(gre_bell)

# v_red_channel = np.vectorize(G.colorizeR)
# v_gre_channel = np.vectorize(G.colorizeG)
# v_blu_channel = np.vectorize(G.colorizeB)

mu_channel = 0
sig_channel = 1
util_channel = 2

util_masks = {
    "visited":1
}

source_mode = False
run_mode = "sigma_mu"
fnt = ImageFont.truetype('arial.ttf', 40)

r_channel = 0
g_channel = 1
b_channel = 2

empty_gray = 200


def rangeMap(min1, max1, val1, min2, max2):
    return min2 + (val1 - min1) * (max2 - min2)/(max1 - min1)

def quad(x, y):
    return ("NE" if y > 0 else "SE") if x > 0 else ("NW" if y > 0 else "SW")

def sign(x):
    return 1 if x >= 0 else -1 

def roundUp(x):
    return math.ceil(x) if x > 0 else math.floor(x)

def roundDown(x):
    return math.ceil(x) if x < 0 else math.floor(x)

coord_dtype = float

parser = argparse.ArgumentParser(description='Retrieve required segment from overlay.')
parser.add_argument('x_min',            type=coord_dtype, nargs=1, help='Minimum X coordinate of bounding box')
parser.add_argument('y_min',            type=coord_dtype, nargs=1, help='Minimum Y coordinate of bounding box')
parser.add_argument('x_max',            type=coord_dtype, nargs=1, help='Maximum X coordinate of bounding box')
parser.add_argument('y_max',            type=coord_dtype, nargs=1, help='Maximum Y coordinate of bounding box')
parser.add_argument('--overlay_folder', type=str,         nargs=1, help='Path to overlay folder')
parser.add_argument('--DEBUG', dest='DEBUG', action='store_const', const=True, default=False)

if DEBUG: print("Args: " + str(sys.argv))

args = parser.parse_args(sys.argv[1:])

DEBUG = DEBUG or args.DEBUG

# Get values from args
req_x_min = min(args.x_min[0], args.x_max[0])
req_x_max = max(args.x_min[0], args.x_max[0])
req_y_min = min(args.y_min[0], args.y_max[0])
req_y_max = max(args.y_min[0], args.y_max[0])

overlay_canvas = segments.load_segments (
    req_x_min, 
    req_y_min, 
    req_x_max, 
    req_y_max, 
    resolution=resolution, 
    alias_append="_"+run_mode, 
    overlay_path=args.overlay_folder[0],
    DEBUG=DEBUG,
    source_mode=source_mode
)

if DEBUG: print("Done!")

aux_canvas = overlay_canvas[:,:,:]

# Convert sigma-mu to gradient
if run_mode == "sigma_mu":
    if DEBUG:
        if (np.max(overlay_canvas[:,:,util_channel]) == 0):
            print("None active!")
        else:
            print("Found active!")
        print("Mu range: {} <> {}".format(np.min(overlay_canvas[:,:,mu_channel]), np.max(overlay_canvas[:,:,mu_channel])))

    _mu = overlay_canvas[:,:,mu_channel] / 255.
    fig = plt.figure(1, figsize=(overlay_canvas.shape[1], overlay_canvas.shape[0]), dpi=1)
    ax = sns.heatmap(np.array(_mu), xticklabels=False, yticklabels=False, cbar=False, cmap=sns.diverging_palette(10, 150, sep=80))
    fig.tight_layout(pad=0)
    fig.canvas.draw()
    data = np.fromstring(fig.canvas.tostring_rgb(), dtype=np.uint8, sep='')
    aux_canvas = data.reshape(fig.canvas.get_width_height()[::-1] + (3,))

    assert aux_canvas.shape == overlay_canvas.shape, "Expected {}, got {}".format(str(overlay_canvas.shape), str(aux_canvas.shape))

    # aux_canvas[:,:,r_channel] = np.uint8(255 - overlay_canvas[:,:,mu_channel])
    # aux_canvas[:,:,g_channel] = np.uint8(127)
    # aux_canvas[:,:,b_channel] = np.uint8(overlay_canvas[:,:,mu_channel])

    # aux_canvas[:,:,r_channel] = np.where(overlay_canvas[:,:,util_channel] == util_masks["visited"], aux_canvas[:,:,r_channel], empty_gray)
    # aux_canvas[:,:,g_channel] = np.where(overlay_canvas[:,:,util_channel] == util_masks["visited"], aux_canvas[:,:,g_channel], empty_gray)
    # aux_canvas[:,:,b_channel] = np.where(overlay_canvas[:,:,util_channel] == util_masks["visited"], aux_canvas[:,:,b_channel], empty_gray)

overlay_canvas_img = Image.fromarray(aux_canvas)

if DEBUG: overlay_canvas_img.show()

#required_corners = segments.expand_corners(req_x_min, req_y_min, req_x_max, req_y_max)

(left, lower, right, upper) = segments.bounding_box(req_x_min, req_y_min, req_x_max, req_y_max, resolution=resolution, dpb=dpb)

if DEBUG: print("Cropping {}, {}, {}, {}".format(left, upper, right, lower))

if DEBUG:
    d = ImageDraw.Draw(overlay_canvas_img)

    ellipse_rad = 10

    d.ellipse([left-ellipse_rad, upper-ellipse_rad, left+ellipse_rad, upper+ellipse_rad],   fill=(255,0,0))
    d.ellipse([right-ellipse_rad, lower-ellipse_rad, right+ellipse_rad, lower+ellipse_rad], fill=(0,0,255))

    overlay_canvas_img.show()

overlay_canvas_img = overlay_canvas_img.crop((left, upper, right, lower))

if DEBUG: overlay_canvas_img.show()

nonce = os.urandom(32).hex()

with open("./tmp/"+nonce+".jpg", "w") as f:
    overlay_canvas_img.save(f, format='JPEG')

print(nonce, end="")

# with io.BytesIO() as output:
#     overlay.save(output, format='PNG')
#     sys.stdout.buffer.write(output.getvalue())

# Testing
segments.save_overlay(
    req_x_min, 
    req_y_min, 
    req_x_max, 
    req_y_max, 
    overlay_canvas, 
    resolution=resolution, 
    dpb=dpb, 
    overlay_path=args.overlay_folder[0],
    DEBUG=DEBUG
)
# graphics_folder = "C:/Users/Usuario/Documents/GitHub/ProjetoIntegrado/SERVER/overlay/graphics/"
# required_corners = segments.expand_corners(req_x_min, req_y_min, req_x_max, req_y_max, resolution=resolution)

# required_delta_x = abs(required_corners[1][0] - required_corners[0][0])
# required_delta_y = abs(required_corners[1][1] - required_corners[0][1])

# __x =  math.ceil(req_x_min / resolution) if req_x_min > 0 else math.floor(req_x_min / resolution)
# __y =  math.ceil(req_y_min / resolution) if req_y_min > 0 else math.floor(req_y_min / resolution)
# for _x in range(required_delta_x):
#     x = _x + __x - segments.__sign(req_x_min)
#     for _y in range(required_delta_y):
#         y = _y + __y - segments.__sign(req_y_min)
#         overlay_alias = "{}_{}_{}{}.png".format(segments.__quad(x, y), abs(y), abs(x), "_sigma_mu")
#         if DEBUG: print("Saving as {}".format(overlay_alias))
#         overlay_segment = Image.fromarray(overlay_canvas[_x*dpb:(_x+1)*dpb,_y*dpb:(_y+1)*dpb,:])
#         overlay_segment.save(graphics_folder+overlay_alias, format="PNG")