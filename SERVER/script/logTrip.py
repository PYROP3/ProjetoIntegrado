from PIL import Image, ImageDraw, ImageFont
import os
import sys
import argparse
import json
import math
import numpy as np
from scipy.ndimage.filters import gaussian_filter

import segments

DEBUG = False

resolution = 0.01
dpb = 500 # Dots per block; each image should be (dpb x dpb)
mu_channel = 0
sig_channel = 1
util_channel = 2

util_masks = {
    "visited":1
}

source_mode = False
fnt = ImageFont.truetype('arial.ttf', 40)

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

parser = argparse.ArgumentParser(description='Store quality information for a series of coordinates.')
parser.add_argument('--coordinates',    type=coords,        nargs='+', help='List of coordinates (x, y)', required=True)
parser.add_argument('--quality',        type=quality_dtype, nargs='+', help='List of quality data',       required=True)
parser.add_argument('--overlay_folder', type=str,           nargs=1,   help='Path to overlay folder',     required=True)
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

overlay_canvas = segments.load_segments(req_x_min, req_y_min, req_x_max, req_y_max, DEBUG=DEBUG)

if DEBUG: print("Done!")

(left, lower, right, upper) = segments.bounding_box(req_x_min, req_y_min, req_x_max, req_y_max, resolution=resolution, dpb=dpb)

if DEBUG: print("Cropping L:{}, U:{}, R:{}, D:{}".format(left, upper, right, lower))
if DEBUG: print("Vert: {}, Horiz: {}".format(lower - upper, right - left))

#update_mask = np.zeros((abs(upper - lower) + (2 * cover_radius) + 2, abs(right - left) + (2 * cover_radius) + 2, 2) - 1
update_mask = np.zeros(overlay_canvas.shape)
update_mask_height, update_mask_width = update_mask.shape

update_mask[:,:,util_channel] = 0

# Generate offsets to cover radius
offsets = []
for _x in range(2 * cover_radius + 1):
    __x = _x - cover_radius
    for _y in range(2 * cover_radius + 1):
        __y = _y - cover_radius
        if ((__x ** 2 + __y ** 2) <= cover_radius ** 2):
            offsets.append([__x, __y])
# print("Generated offsets: " + str(offsets))

required_corners = segments.expand_corners(req_x_min, req_y_min, req_x_max, req_y_max, resolution=resolution)

required_delta_x = abs(required_corners[1][0] - required_corners[0][0])
required_delta_y = abs(required_corners[1][1] - required_corners[0][1])

# Iterate over all the quality data
for i in range(len(args.quality)):
    q = args.quality[i]
    #segment_points = []
    p0 = args.coordinates[i]
    p1 = args.coordinates[i+1]
    print("P0 = " + str(p0))
    x00 = int(segments.__rangeMap(required_corners[0][0], required_corners[1][0], p0[0] / resolution, 0, required_delta_x * dpb))
    y00 = int(segments.__rangeMap(required_corners[0][1], required_corners[1][1], p0[1] / resolution, required_delta_y * dpb, 0))
    x0  = x00
    y0  = y00
    x1  = int(segments.__rangeMap(required_corners[0][0], required_corners[1][0], p1[0] / resolution, 0, required_delta_x * dpb))
    y1  = int(segments.__rangeMap(required_corners[0][1], required_corners[1][1], p1[1] / resolution, required_delta_y * dpb, 0))
    if DEBUG: print("[{}] Processing q={} @ ({},{}) - ({},{}) Orig ({} x {})".format(i, q, x0, y0, x1, y1, str(p0), str(p1)))
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
            if (nx >= 0 and nx < update_mask_width and ny >= 0 and ny < update_mask_height):
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

# Apply gaussian filter over mask to spread quality data (?)
#update_mask = update_mask * gauss_factor
gaussed = update_mask
# for _ in range(gauss_runs):
#     gaussed = gaussian_filter(gaussed, sigma=gauss_sigma)

print("Mu  mask min = {}, max = {}".format(np.min(gaussed[:,:,mu_channel]), np.max(gaussed[:,:,mu_channel])))
print("Sig mask min = {}, max = {}".format(np.min(gaussed[:,:,sig_channel]), np.max(gaussed[:,:,sig_channel])))

mu_mask  = np.uint8(gaussed[:,:,mu_channel]*255) #1:-1,1:-1
sig_mask = np.uint8(np.sqrt(gaussed[:,:,mu_channel])*255)
overlay_canvas[:,:,   mu_channel] = np.where(update_mask[:,:,util_channel] == util_masks["visited"],  mu_mask, overlay_canvas[:,:,  mu_channel]) 
overlay_canvas[:,:,  sig_channel] = np.where(update_mask[:,:,util_channel] == util_masks["visited"], sig_mask, overlay_canvas[:,:, sig_channel]) 
overlay_canvas[:,:, util_channel] = update_mask[:,:,util_channel]

if (np.max(overlay_canvas[:,:,util_channel]) == 0):
    print("None active!")
else:
    print("Found active!")

print("Mu range: {} <> {}".format(np.min(overlay_canvas[:,:,mu_channel]), np.max(overlay_canvas[:,:,mu_channel])))

# Convert back to image
overlay_canvas_img = Image.fromarray(overlay_canvas)
if DEBUG: overlay_canvas_img.show()

# _ = input("Press enter to continue ")

# Split back into pieces and overwrite disk data
__x =  math.ceil(req_x_min / resolution) if req_x_min > 0 else math.floor(req_x_min / resolution)
__y =  math.ceil(req_y_min / resolution) if req_y_min > 0 else math.floor(req_y_min / resolution)
for _x in range(required_delta_x):
    x = _x + __x - segments.__sign(req_x_min)
    for _y in range(required_delta_y):
        y = _y + __y - segments.__sign(req_y_min)
        overlay_alias = "{}_{}_{}{}.png".format(segments.__quad(x, y), abs(x), abs(y), test_mode)
        if DEBUG: print("Saving as {}".format(overlay_alias))
        overlay_segment = Image.fromarray(overlay_canvas[_y*dpb:(_y+1)*dpb,_x*dpb:(_x+1)*dpb,:])
        overlay_segment.save(graphics_folder+overlay_alias, format="PNG")
        

# with io.BytesIO() as output:
#     overlay.save(output, format='PNG')
#     sys.stdout.buffer.write(output.getvalue())