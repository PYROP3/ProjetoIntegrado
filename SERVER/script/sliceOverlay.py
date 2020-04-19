from PIL import Image, ImageDraw
import os
import sys
import argparse
import json
import math
import numpy as np

DEBUG = False

resolution = 0.01
dpb = 500 # Dots per block; each image should be (dpb x dpb)

def red_sigmoid(x):
    return 1. / (1. + math.exp(10*x-5))

v_red_sigmoid = np.vectorize(red_sigmoid)

def blu_sigmoid(x):
    return 1. / (1. + math.exp(5-10*x))

v_blu_sigmoid = np.vectorize(blu_sigmoid)

def gre_bell(x):
    return 0.5*math.exp(-10*((x-0.5)**2))

v_gre_bell = np.vectorize(gre_bell)

mu_channel = 0
sig_channel = 1
util_channel = 2

util_masks = {
    "visited":1
}

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
parser.add_argument('x_min', type=coord_dtype, nargs=1, help='Minimum X coordinate of bounding box')
parser.add_argument('y_min', type=coord_dtype, nargs=1, help='Minimum Y coordinate of bounding box')
parser.add_argument('x_max', type=coord_dtype, nargs=1, help='Maximum X coordinate of bounding box')
parser.add_argument('y_max', type=coord_dtype, nargs=1, help='Maximum Y coordinate of bounding box')
parser.add_argument('overlay_folder', type=str, nargs=1, help='Path to overlay folder')
parser.add_argument('overlay', type=str, nargs=1, help='Path to overlay JSON metadata')
parser.add_argument('--DEBUG', dest='DEBUG', action='store_const', const=True, default=False)

if DEBUG: print("Args: " + str(sys.argv))

args = parser.parse_args(sys.argv[1:])

DEBUG = args.DEBUG

# Get values from args
req_x_min = min(args.x_min[0], args.x_max[0])
req_x_max = max(args.x_min[0], args.x_max[0])
req_y_min = min(args.y_min[0], args.y_max[0])
req_y_max = max(args.y_min[0], args.y_max[0])

# sign_x_min = sign(req_x_min)
# sign_x_max = sign(req_x_max)
# sign_y_min = sign(req_y_min)
# sign_y_max = sign(req_y_max)

point_quads = [
    quad(req_x_min, req_y_min),
    quad(req_x_max, req_y_max)
]

required_corners = [
    [math.floor(req_x_min / resolution), math.floor(req_y_min / resolution)],
    [math.ceil(req_x_max / resolution), math.ceil(req_y_max / resolution)]
]

if DEBUG: print(point_quads[0] + " : " + str(required_corners[0]))
if DEBUG: print(point_quads[1] + " : " + str(required_corners[1]))

required_delta_x = abs(required_corners[1][0] - required_corners[0][0]) + 0
required_delta_y = abs(required_corners[1][1] - required_corners[0][1]) + 0

if DEBUG: print("Deltas: " + str(required_delta_x) + ", " + str(required_delta_y))

overlay_canvas = np.zeros((required_delta_y * dpb, required_delta_x * dpb, 3), dtype=np.uint8)

if DEBUG: print("Overlay shape: " + str(overlay_canvas.shape))

__x =  math.ceil(req_x_min / resolution) if req_x_min > 0 else math.floor(req_x_min / resolution)
__y =  math.ceil(req_y_min / resolution) if req_y_min > 0 else math.floor(req_y_min / resolution)
# __x_ = math.ceil(req_x_max / resolution) if req_x_max > 0 else math.floor(req_x_max / resolution)
# __y_ = math.ceil(req_y_max / resolution) if req_y_max > 0 else math.floor(req_y_max / resolution)
for _x in range(required_delta_x):
    x = _x + __x - sign(req_x_min)
    for _y in range(required_delta_y):
        y = _y + __y - sign(req_y_min)
        overlay_alias = "{}_{}_{}_sigma_mu.png".format(quad(x, y), abs(x), abs(y))
        if DEBUG: print("Opening {}".format(overlay_alias))
        with Image.open(args.overlay_folder[0]+"graphics/"+overlay_alias).convert("RGB") as temp_overlay:
            aux = np.asarray(temp_overlay)
            if DEBUG: print("Ranges: {}:{}, {}:{}".format(_y*dpb,(_y+1)*dpb,_x*dpb,(_x+1)*dpb))
            overlay_canvas[_y*dpb:(_y+1)*dpb,_x*dpb:(_x+1)*dpb,:] = aux

if DEBUG: print("Done!")

# Convert sigma-mu to gradient

if (np.max(overlay_canvas[:,:,util_channel]) == 0):
    print("None active!")
else:
    print("Found active!")

print("Mu range: {} <> {}".format(np.min(overlay_canvas[:,:,mu_channel]), np.max(overlay_canvas[:,:,mu_channel])))

aux_canvas = overlay_canvas[:,:,:]
aux_canvas[:,:,r_channel] = np.where(overlay_canvas[:,:,util_channel] == util_masks["visited"], v_red_sigmoid(overlay_canvas[:,:,mu_channel] / 255.), empty_gray)
aux_canvas[:,:,g_channel] = np.where(overlay_canvas[:,:,util_channel] == util_masks["visited"],    v_gre_bell(overlay_canvas[:,:,mu_channel] / 255.), empty_gray)
aux_canvas[:,:,b_channel] = np.where(overlay_canvas[:,:,util_channel] == util_masks["visited"], v_blu_sigmoid(overlay_canvas[:,:,mu_channel] / 255.), empty_gray)

overlay_canvas_img = Image.fromarray(aux_canvas)

if DEBUG: overlay_canvas_img.show()

# print("__x = {}, __x_ = {}".format(__x, __x_))
# print("__y = {}, __y_ = {}".format(__y, __y_))

(left, lower, right, upper) = (
    int(rangeMap(required_corners[0][0], required_corners[1][0], req_x_min / resolution, 0, required_delta_x * dpb)),
    int(rangeMap(required_corners[0][1], required_corners[1][1], req_y_min / resolution, required_delta_y * dpb, 0)),
    int(rangeMap(required_corners[0][0], required_corners[1][0], req_x_max / resolution, 0, required_delta_x * dpb)),
    int(rangeMap(required_corners[0][1], required_corners[1][1], req_y_max / resolution, required_delta_y * dpb, 0))
)

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