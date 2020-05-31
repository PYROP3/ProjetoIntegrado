from PIL import Image, ImageDraw, ImageFont
import os
import sys
import argparse
import json
import math
import numpy as np
from scipy.ndimage.filters import gaussian_filter

from errorHandler import ErrorHandler
import segments

DEBUG = False

mu_channel = 0
sig_channel = 1
util_channel = 2

util_masks = {
    "visited":1
}

source_mode = False
run_mode = "sigma_mu"

e_sq = math.e**2
gauss_sigma = 7
gauss_factor = 10
gauss_runs = 3

brush_radius = 5

def coord_list_dtype(s):
    try:
        x = list(map(lambda p: list(map(float, p.split(',')))[::-1], s.split(' ')))
        return x
    except:
        raise argparse.ArgumentTypeError(
            "Coordinates must be (x0,y0 x1,y1 ... xn,yn) :[{}]".format(s)
            )

def accel_dtype(s):
    try:
        x = list(map(lambda p: list(map(float, p.split(',')))[::-1], s.split(' ')))
        return x
    except:
        raise argparse.ArgumentTypeError(
            "Qualities must be (x0,y0,z0 x1,y1,z1 ... xn,yn,zn) :[{}]".format(s)
            )

def pseudoSigmoid(value, k=10):
    return 2 - 2 / (1 + math.exp(-value / k))

def convertAccelerometerData(data):
    _mags = [np.fft.fft(
                [tupla[0]**2 + tupla[1]**2 + tupla[2]**2 for tupla in segment]
            ) for segment in data]
    _res = [np.fft.fftfreq(len(x))[np.argmax(np.abs(x))] for x in _mags]
    return [pseudoSigmoid(freq) for freq in _res]

parser = argparse.ArgumentParser(description='Store quality information for a series of coordinates.')
parser.add_argument('--coordinates', type=coord_list_dtype, nargs='+', help='List of coordinates (x, y)', required=True)
parser.add_argument('-d', '--accel_data', type=accel_dtype, nargs='+', help='List of accelerometer data', required=True, action='append')
parser.add_argument('--overlay_folder', type=str, nargs=1, help='Path to overlay folder', required=True)
parser.add_argument('--errors_file', type=str, nargs=1, help='Path to errors JSON file')
parser.add_argument('--DEBUG', dest='DEBUG', action='store_const', const=True, default=False)

if DEBUG: print("Args: " + str(sys.argv))

try:
    args = parser.parse_args(sys.argv[1:])
except argparse.ArgumentTypeError:
    exit(99)

err = ErrorHandler(args.errors_file[0])

DEBUG = args.DEBUG

if DEBUG: print("Arg parsed: " + str(args))

global_coordinates = args.coordinates[0]
global_accelData = [x[0] for x in args.accel_data] #FIXME
# global_quality = args.quality[0]

# Check for mismatched coord and quality lists
if (len(global_coordinates) != len(global_accelData) + 1): err.exitOnError("MismatchedData")

# Find bounding box of all coordinates
req_x_min = min([c[0] for c in global_coordinates])
req_y_min = min([c[1] for c in global_coordinates])
req_x_max = max([c[0] for c in global_coordinates])
req_y_max = max([c[1] for c in global_coordinates])

if DEBUG: print(global_accelData)

global_quality = convertAccelerometerData(global_accelData)
if DEBUG: print(global_quality)

if DEBUG: print("Bounding box from {}, {} to {}, {}".format(req_x_min, req_y_min, req_x_max, req_y_max))

overlay_canvas = segments.load_segments (
    req_x_min,
    req_y_min,
    req_x_max,
    req_y_max,
    alias_append="_"+run_mode,
    overlay_path=args.overlay_folder[0],
    DEBUG=DEBUG,
    source_mode=source_mode
)
if DEBUG: print("Done!")

(left, lower, right, upper) = segments.bounding_box(req_x_min, req_y_min, req_x_max, req_y_max)

if DEBUG: print("Cropping L:{}, U:{}, R:{}, D:{}".format(left, upper, right, lower))
if DEBUG: print("Vert: {}, Horiz: {}".format(lower - upper, right - left))

try:
    update_mask = np.zeros(overlay_canvas.shape)
except ValueError:
    err.exitOnError("MemoryError")

update_mask_height, update_mask_width, _ = update_mask.shape

update_mask[:, :, util_channel] = 0

# Generate offsets to cover radius
offsets = []
for _x in range(2 * brush_radius + 1):
    __x = _x - brush_radius
    for _y in range(2 * brush_radius + 1):
        __y = _y - brush_radius
        if ((__x ** 2 + __y ** 2) <= brush_radius ** 2):
            offsets.append([__x, __y])
if DEBUG: print("Generated offsets: " + str(offsets))

required_corners = segments.expand_corners(req_x_min, req_y_min, req_x_max, req_y_max)

required_delta_x = abs(required_corners[1][0] - required_corners[0][0])
required_delta_y = abs(required_corners[1][1] - required_corners[0][1])

# Iterate over all the quality data
for i, elem in enumerate(global_quality):
    q = global_quality[i]
    p0 = global_coordinates[i]
    p1 = global_coordinates[i+1]
    if DEBUG: print("P0 = " + str(p0))

    x00 = int(segments.__rangeMap(required_corners[0][0], required_corners[1][0], p0[0] / segments.resolution, 0, required_delta_x * segments.dpb))
    y00 = int(segments.__rangeMap(required_corners[0][1], required_corners[1][1], p0[1] / segments.resolution, required_delta_y * segments.dpb, 0))
    x0 = x00
    y0 = y00
    x1 = int(segments.__rangeMap(required_corners[0][0], required_corners[1][0], p1[0] / segments.resolution, 0, required_delta_x * segments.dpb))
    y1 = int(segments.__rangeMap(required_corners[0][1], required_corners[1][1], p1[1] / segments.resolution, required_delta_y * segments.dpb, 0))
    if DEBUG: print("[{}] Processing q={} @ ({}, {}) - ({}, {}) Orig ({} x {})".format(i, q, x0, y0, x1, y1, str(p0), str(p1)))

    # Bresenhams algorithm
    dx = abs(x1 - x0)
    sx = 1 if x0 < x1 else -1
    dy = -abs(y1 - y0)
    sy = 1 if y0 < y1 else -1
    err = dx + dy
    if DEBUG: print("Bresenham: dx={}, sx={}, dy={}, sy={}, err={}".format(dx, sx, dy, sy, err))
    while (x1 != x0 or y1 != y0):
        # Process (x0, y0)
        canvas_x = x0
        canvas_y = y0
        prev_mu = float(overlay_canvas[canvas_y, canvas_x, mu_channel])/255.
        prev_sig = float((overlay_canvas[canvas_y, canvas_x, sig_channel])/255.)**2 # Stored as square root
        if DEBUG: print("Canvas @ {}, {} => mu = {}[sto {}]; sig = {}[sto {}]".format(canvas_x, canvas_y, prev_mu, overlay_canvas[canvas_y, canvas_x, mu_channel], prev_sig, overlay_canvas[canvas_y, canvas_x, sig_channel]))
        new_mu = (prev_sig * q + e_sq * prev_mu) / (prev_sig + e_sq)
        new_sig = (prev_sig * e_sq              ) / (prev_sig + e_sq)
        update_mask[y0, x0, mu_channel] = new_mu
        update_mask[y0, x0, sig_channel] = new_sig
        update_mask[y0, x0, util_channel] = util_masks["visited"]
        for o in offsets:
            nx = x0 + o[0] - 1
            ny = y0 + o[1] - 1
            if (nx >= 0 and nx < update_mask_width and ny >= 0 and ny < update_mask_height):
                update_mask[ny, nx, mu_channel] = new_mu
                update_mask[ny, nx, sig_channel] = new_sig
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

if DEBUG:
    print("Mu  mask min = {}, max = {}".format(np.min(gaussed[:, :, mu_channel]), np.max(gaussed[:, :, mu_channel])))
    print("Sig mask min = {}, max = {}".format(np.min(gaussed[:, :, sig_channel]), np.max(gaussed[:, :, sig_channel])))

mu_mask = np.uint8(gaussed[:, :, mu_channel]*255) #1:-1, 1:-1
sig_mask = np.uint8(np.sqrt(gaussed[:, :, mu_channel])*255)
overlay_canvas[:, :, mu_channel] = np.where(update_mask[:, :, util_channel] == util_masks["visited"], mu_mask, overlay_canvas[:, :, mu_channel])
overlay_canvas[:, :, sig_channel] = np.where(update_mask[:, :, util_channel] == util_masks["visited"], sig_mask, overlay_canvas[:, :, sig_channel])
overlay_canvas[:, :, util_channel] = update_mask[:, :, util_channel]

if DEBUG:
    if (np.max(overlay_canvas[:, :, util_channel]) == 0):
        print("None active!")
    else:
        print("Found active!")

    print("Mu range: {} <> {}".format(np.min(overlay_canvas[:, :, mu_channel]), np.max(overlay_canvas[:, :, mu_channel])))

# Convert back to image
if DEBUG:
    overlay_canvas_img = Image.fromarray(overlay_canvas)
    overlay_canvas_img.show()

# Split back into pieces and overwrite disk data
segments.save_overlay(
    req_x_min,
    req_y_min,
    req_x_max,
    req_y_max,
    overlay_canvas,
    overlay_path=args.overlay_folder[0],
    DEBUG=DEBUG
)

# with io.BytesIO() as output:
#     overlay.save(output, format='PNG')
#     sys.stdout.buffer.write(output.getvalue())