from PIL import Image, ImageDraw, ImageFont
import os
import sys
import argparse
import json
import math
import numpy as np
import seaborn as sns; sns.set()
from matplotlib import pyplot as plt

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
parser.add_argument('x_min', type=coord_dtype, nargs=1, help='Minimum X coordinate of bounding box')
parser.add_argument('y_min', type=coord_dtype, nargs=1, help='Minimum Y coordinate of bounding box')
parser.add_argument('x_max', type=coord_dtype, nargs=1, help='Maximum X coordinate of bounding box')
parser.add_argument('y_max', type=coord_dtype, nargs=1, help='Maximum Y coordinate of bounding box')
parser.add_argument('--overlay_folder', type=str, nargs=1, help='Path to overlay folder')
parser.add_argument('--errors_file', type=str, nargs=1, help='Path to errors JSON file')
parser.add_argument('--DEBUG', dest='DEBUG', action='store_const', const=True, default=False)

if DEBUG: print("Args: " + str(sys.argv))

try:
    args = parser.parse_args(sys.argv[1:])
except argparse.ArgumentTypeError:
    exit(99)

err = ErrorHandler(args.errors_file[0])

DEBUG = DEBUG or args.DEBUG

# Get values from args
req_x_min = args.x_min[0]
req_x_max = args.x_max[0]
req_y_min = args.y_min[0]
req_y_max = args.y_max[0]

try:
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
except MemoryError:
    err.exitOnError("MemoryError")

if req_x_min < -180 or req_x_max > 180 or req_y_max > 90 or req_y_min < -90:
    raise err.exitOnError("LimitsError")

if req_x_min > req_x_max  or req_y_max < req_y_min:
    raise err.exitOnError("InvertedValues")

if DEBUG: print("Done!")

aux_canvas = overlay_canvas[:, :, :]

# Convert sigma-mu to gradient
if run_mode == "sigma_mu":
    if DEBUG:
        if (np.max(overlay_canvas[:, :, util_channel]) == 0):
            print("None active!")
        else:
            print("Found active!")
        print("Mu range: {} <> {}".format(np.min(overlay_canvas[:, :, mu_channel]), np.max(overlay_canvas[:, :, mu_channel])))

    try:
        _mu = overlay_canvas[:, :, mu_channel] / 255.
    except MemoryError:
        err.exitOnError("MemoryError")

    fig = plt.figure(1, figsize=(overlay_canvas.shape[1], overlay_canvas.shape[0]), dpi=1)
    ax = sns.heatmap(np.array(_mu), xticklabels=False, yticklabels=False, cbar=False, cmap=sns.diverging_palette(10, 150, sep=80))
    fig.tight_layout(pad=0)
    fig.canvas.draw()
    data = np.frombuffer(fig.canvas.tostring_rgb(), dtype=np.uint8)
    aux_canvas = data.reshape(fig.canvas.get_width_height()[::-1] + (3, ))

    assert aux_canvas.shape == overlay_canvas.shape, "Expected {}, got {}".format(str(overlay_canvas.shape), str(aux_canvas.shape))

overlay_canvas_img = Image.fromarray(aux_canvas)

if DEBUG: overlay_canvas_img.show()

(left, lower, right, upper) = segments.bounding_box(req_x_min, req_y_min, req_x_max, req_y_max)

if DEBUG: print("Cropping {}, {}, {}, {}".format(left, upper, right, lower))

if DEBUG:
    d = ImageDraw.Draw(overlay_canvas_img)

    ellipse_rad = 10

    d.ellipse([left-ellipse_rad, upper-ellipse_rad, left+ellipse_rad, upper+ellipse_rad], fill=(255, 0, 0))
    d.ellipse([right-ellipse_rad, lower-ellipse_rad, right+ellipse_rad, lower+ellipse_rad], fill=(0, 0, 255))

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
# segments.save_overlay(
#     req_x_min,
#     req_y_min,
#     req_x_max,
#     req_y_max,
#     overlay_canvas,
#     resolution=resolution,
#     dpb=dpb,
#     overlay_path=args.overlay_folder[0],
#     DEBUG=DEBUG
# )