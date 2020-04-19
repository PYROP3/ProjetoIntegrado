from PIL import Image, ImageDraw
import os
import sys
import argparse
import json
import math
import numpy as np
from scipy.ndimage.filters import gaussian_filter

DEBUG = False

resolution = 0.01
dpb = 500 # Dots per block; each image should be (dpb x dpb)
mu_channel = 0
sig_channel = 1
util_channel = 2

util_masks = {
    "visited":1
}

e_sq = math.e**2
gauss_sigma  = 7
gauss_factor = 10
gauss_runs   = 3

cover_radius = 5

test_mode = "_sigma_mu"

graphics_folder = "C:/Users/Usuario/Documents/GitHub/ProjetoIntegrado/SERVER/overlay/graphics/"

def coords(s):
    try:
        lat, lon = map(float, s.split(' '))
        return lon, lat
    except:
        raise argparse.ArgumentTypeError("Coordinates must be x,y :[{}]".format(s))

quality_dtype = float

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

parser = argparse.ArgumentParser(description='Store quality information for a series of coordinates.')
parser.add_argument('--coordinates', type=coords, nargs='+', help='List of coordinates (x, y)', required=True)
parser.add_argument('--quality', type=quality_dtype, nargs='+', help='List of quality data', required=True)
parser.add_argument('--DEBUG', dest='DEBUG', action='store_const', const=True, default=False)

if DEBUG: print("Args: " + str(sys.argv))

args = parser.parse_args(sys.argv[1:])

DEBUG = args.DEBUG

# Check for mismatched coord and quality lists
if (len(args.coordinates) != len(args.quality) + 1): exit(1)

# Find bounding box of all coordinates
req_x_min = min([c[0] for c in args.coordinates])
req_y_min = min([c[1] for c in args.coordinates])
req_x_max = max([c[0] for c in args.coordinates])
req_y_max = max([c[1] for c in args.coordinates])

if DEBUG: print("Bounding box from {},{} to {},{}".format(req_x_min, req_y_min, req_x_max, req_y_max))

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
for _x in range(required_delta_x):
    x = _x + __x - sign(req_x_min)
    for _y in range(required_delta_y):
        y = _y + __y - sign(req_y_min)
        overlay_alias = "{}_{}_{}{}.png".format(quad(x, y), abs(x), abs(y), test_mode)
        with Image.open(graphics_folder+overlay_alias).convert("RGB") as temp_overlay:
            aux = np.asarray(temp_overlay)
            if DEBUG: print("Ranges: {}:{}, {}:{}".format(_y*dpb,(_y+1)*dpb,_x*dpb,(_x+1)*dpb))
            overlay_canvas[_y*dpb:(_y+1)*dpb,_x*dpb:(_x+1)*dpb,:] = aux

if DEBUG: print("Done!")

(left, lower, right, upper) = (
    int(rangeMap(required_corners[0][0], required_corners[1][0], req_x_min / resolution, 0, required_delta_x * dpb)),
    int(rangeMap(required_corners[0][1], required_corners[1][1], req_y_min / resolution, required_delta_y * dpb, 0)),
    int(rangeMap(required_corners[0][0], required_corners[1][0], req_x_max / resolution, 0, required_delta_x * dpb)),
    int(rangeMap(required_corners[0][1], required_corners[1][1], req_y_max / resolution, required_delta_y * dpb, 0))
)
if DEBUG: print("Cropping L:{}, U:{}, R:{}, D:{}".format(left, upper, right, lower))
if DEBUG: print("Vert: {}, Horiz: {}".format(lower - upper, right - left))

#update_mask = np.zeros((abs(upper - lower) + (2 * cover_radius) + 2, abs(right - left) + (2 * cover_radius) + 2, 2) - 1
update_mask = np.zeros(overlay_canvas.shape) - 1

# Generate offsets to cover radius
offsets = []
for _x in range(2 * cover_radius + 1):
    __x = _x - cover_radius
    for _y in range(2 * cover_radius + 1):
        __y = _y - cover_radius
        if ((__x ** 2 + __y ** 2) <= cover_radius ** 2):
            offsets.append([__x, __y])
#rint("Generated offsets: " + str(offsets))

# Iterate over all the quality data
for i in range(len(args.quality)):
    q = args.quality[i]
    #segment_points = []
    p0 = args.coordinates[i]
    p1 = args.coordinates[i+1]
    print("P0 = " + str(p0))
    x00 = int(rangeMap(required_corners[0][0], required_corners[1][0], p0[0] / resolution, 0, required_delta_x * dpb))
    y00 = int(rangeMap(required_corners[0][1], required_corners[1][1], p0[1] / resolution, required_delta_y * dpb, 0))
    x0 = x00
    y0 = y00
    x1 = int(rangeMap(required_corners[0][0], required_corners[1][0], p1[0] / resolution, 0, required_delta_x * dpb))
    y1 = int(rangeMap(required_corners[0][1], required_corners[1][1], p1[1] / resolution, required_delta_y * dpb, 0))
    if DEBUG: print("[{}] Processing q={} @ ({},{}) - ({},{}) Orig ({} x {})".format(i, q, x0, y0, x1, y1, x00, y00, str(p0), str(p1)))
    # Bresenhams algorithm
    dx = abs(x1 - x0)
    sx = 1 if x0 < x1 else -1
    dy = -abs(y1 - y0)
    sy = 1 if y0 < y1 else -1
    err = dx + dy
    print("Bresenham: dx={}, sx={}, dy={}, sy={}, err={}".format(dx, sx, dy, sy, err))
    while (x1 != x0 or y1 != y0):
        # Process (x0, y0)
        canvas_x = x0
        canvas_y = y0
        prev_mu  = float(overlay_canvas[canvas_y,canvas_x,mu_channel])/255.
        prev_sig = float((overlay_canvas[canvas_y,canvas_x,sig_channel])/255.)**2 # Stored as square root
        if DEBUG: print("Canvas @ {},{} => mu = {}[sto {}]; sig = {}[sto {}]".format(canvas_x, canvas_y, prev_mu, overlay_canvas[canvas_y,canvas_x,mu_channel], prev_sig,overlay_canvas[canvas_y,canvas_x,sig_channel]))
        new_mu   = (prev_sig * q + e_sq * prev_mu) / (prev_sig + e_sq)
        new_sig  = (prev_sig * e_sq              ) / (prev_sig + e_sq)
        #print("Updating mask @ {},{}".format(x0, y0))
        update_mask[y0, x0,   mu_channel] = new_mu
        update_mask[y0, x0,  sig_channel] = new_sig
        update_mask[y0, x0, util_channel] = util_masks["visited"]
        for o in offsets:
            nx = x0 + o[0] - 1
            ny = y0 + o[1] - 1
            #print("Testing point {},{}".format(nx, ny))
            if (nx > 0 and nx <= update_mask.shape[1] and ny > 0 and ny <= update_mask.shape[0]):
                update_mask[ny, nx,   mu_channel] = new_mu
                update_mask[ny, nx,  sig_channel] = new_sig
                update_mask[ny, nx, util_channel] = util_masks["visited"]
        if DEBUG: print("Canvas updated => mu = {}[sto {}]; sig = {}[sto {}]".format(new_mu, np.uint8(new_mu * 255), new_sig, np.uint8(np.sqrt(new_sig) * 255)))
        e2 = 2*err 
        if (e2 >= dy):
            err += dy 
            x0 += sx 
        if (e2 <= dx):
            err += dx 
            y0 += sy 

# Apply gaussian filter over mask to spread quality data
#update_mask = update_mask * gauss_factor
gaussed = update_mask
# for _ in range(gauss_runs):
#     gaussed = gaussian_filter(gaussed, sigma=gauss_sigma)

mu_mask  = np.uint8(gaussed[:,:,mu_channel]*255) #1:-1,1:-1
print("Sig mask min = {}, max = {}".format(np.min(gaussed[:,:,mu_channel]), np.max(gaussed[:,:,mu_channel])))
sig_mask = np.uint8(np.sqrt(gaussed[:,:,mu_channel])*255)
# print("Shapes: ")
# print("Canvas: " + str(overlay_canvas.shape))
# print("Cropped canvas: " + str(overlay_canvas[upper:lower, left:right,  mu_channel].shape))
# print("Mu_mask: " + str(mu_mask.shape))
# overlay_canvas[upper-cover_radius:lower+cover_radius, left-cover_radius:right+cover_radius,  mu_channel] = np.where( mu_mask > 0,  mu_mask, overlay_canvas[upper-cover_radius:lower+cover_radius, left-cover_radius:right+cover_radius,  mu_channel]) 
# overlay_canvas[upper-cover_radius:lower+cover_radius, left-cover_radius:right+cover_radius, sig_channel] = np.where(sig_mask > 0, sig_mask, overlay_canvas[upper-cover_radius:lower+cover_radius, left-cover_radius:right+cover_radius, sig_channel]) 
overlay_canvas[:,:,  mu_channel] = np.where( mu_mask > 0,  mu_mask, overlay_canvas[:,:,  mu_channel]) 
overlay_canvas[:,:, sig_channel] = np.where(sig_mask > 0, sig_mask, overlay_canvas[:,:, sig_channel]) 

# Convert back to image
overlay_canvas_img = Image.fromarray(overlay_canvas)
if DEBUG: overlay_canvas_img.show()

# _ = input("Press enter to continue ")

# Split back into pieces and overwrite disk data
__x =  math.ceil(req_x_min / resolution) if req_x_min > 0 else math.floor(req_x_min / resolution)
__y =  math.ceil(req_y_min / resolution) if req_y_min > 0 else math.floor(req_y_min / resolution)
for _x in range(required_delta_x):
    x = _x + __x - sign(req_x_min)
    for _y in range(required_delta_y):
        y = _y + __y - sign(req_y_min)
        overlay_alias = "{}_{}_{}{}_save.png".format(quad(x, y), abs(x), abs(y), test_mode)
        if DEBUG: print("Saving as {}".format(overlay_alias))
        overlay_segment = Image.fromarray(overlay_canvas[_y*dpb:(_y+1)*dpb,_x*dpb:(_x+1)*dpb,:])
        overlay_segment.save(graphics_folder+overlay_alias, format="PNG")
        

# with io.BytesIO() as output:
#     overlay.save(output, format='PNG')
#     sys.stdout.buffer.write(output.getvalue())