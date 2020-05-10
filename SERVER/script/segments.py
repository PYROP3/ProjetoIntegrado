from PIL import Image, ImageDraw, ImageFont
import os
import sys
import argparse
import json
import math
import numpy as np

resolution = 0.01 # Arclength (in degrees) corresponding to each segment side - 0.01deg ~ 1Km
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

source_mode = False
try:
    fnt = ImageFont.truetype('arial.ttf', 40)
except OSError:
    fnt = ImageFont.load_default()

r_channel = 0
g_channel = 1
b_channel = 2

empty_gray = 200

def __rangeMap(min1, max1, val1, min2, max2):
    return min2 + (val1 - min1) * (max2 - min2)/(max1 - min1)

def __quad(x, y):
    return ("NE" if y > 0 else "SE") if x > 0 else ("NW" if y > 0 else "SW")

def __sign(x):
    return 1 if x >= 0 else -1

# def roundUp(x):
#     return math.ceil(x) if x > 0 else math.floor(x)

# def roundDown(x):
#     return math.ceil(x) if x < 0 else math.floor(x)

def expand_corners(min_x, min_y, max_x, max_y, resolution=resolution):
    return [
        [math.floor(min_x / resolution), math.floor(min_y / resolution)],
        [math.ceil(max_x / resolution), math.ceil(max_y / resolution)]
    ]

def load_segments(min_x, min_y, max_x, max_y, resolution=resolution, alias_append="", dpb=dpb, overlay_path="", DEBUG=False, source_mode=False):

    required_corners = expand_corners(min_x, min_y, max_x, max_y, resolution=resolution)

    required_delta_x = abs(required_corners[1][0] - required_corners[0][0]) + 0
    required_delta_y = abs(required_corners[1][1] - required_corners[0][1]) + 0

    try:
        overlay_canvas = np.zeros((required_delta_y * dpb, required_delta_x * dpb, 3), dtype=np.uint8)
    except ValueError:
        raise MemoryError

    __x =  math.ceil(min_x / resolution) if min_x > 0 else math.floor(min_x / resolution)
    __y =  math.ceil(min_y / resolution) if min_y > 0 else math.floor(min_y / resolution)

    for _x in range(required_delta_x):
        x = _x + __x - __sign(min_x)
        for _y in range(required_delta_y):
            y = _y + __y - __sign(min_y)
            overlay_alias = "{}_{}_{}{}".format(__quad(x, y), abs(y), abs(x), alias_append)
            if DEBUG: print("Opening {}".format(overlay_alias))
            try: # Get segment if it exists
                with Image.open(overlay_path+"graphics/"+overlay_alias+".png").convert("RGB") as temp_overlay:
                    aux = np.asarray(temp_overlay)
                    if DEBUG: print("Ranges: {}:{}, {}:{}".format(_y*dpb, (_y+1)*dpb, _x*dpb, (_x+1)*dpb))
                    overlay_canvas[_y*dpb:(_y+1)*dpb, _x*dpb:(_x+1)*dpb, :] = aux[:, :, :]
            except: # Create new segment if not found on disk
                if DEBUG: print("[PYTHON] creating new segment (source_mode = {})".format(str(source_mode)))
                if source_mode: # Source mode (for testing cropping)
                    img = Image.new("RGB", (dpb, dpb), ((x+y)%2*255, 0, (x+y)%2*255))
                    d = ImageDraw.Draw(img)
                    d.text((20, 20), overlay_alias, font=fnt, fill=(127, 127, 127, 255))
                else: # Sigma-mu mode (for testing maths)
                    img = Image.new("RGB", (dpb, dpb), (127, 255, 0))
                aux = np.asarray(img)
                overlay_canvas[_y*dpb:(_y+1)*dpb, _x*dpb:(_x+1)*dpb, :] = aux[:, :, :]

    return overlay_canvas

def bounding_box(min_x, min_y, max_x, max_y, resolution=resolution, dpb=dpb):
    required_corners = expand_corners(min_x, min_y, max_x, max_y, resolution=resolution)

    required_delta_x = abs(required_corners[1][0] - required_corners[0][0])
    required_delta_y = abs(required_corners[1][1] - required_corners[0][1])

    return (
        int(__rangeMap(required_corners[0][0], required_corners[1][0], min_x / resolution, 0, required_delta_x * dpb)),
        int(__rangeMap(required_corners[0][1], required_corners[1][1], min_y / resolution, required_delta_y * dpb, 0)),
        int(__rangeMap(required_corners[0][0], required_corners[1][0], max_x / resolution, 0, required_delta_x * dpb)),
        int(__rangeMap(required_corners[0][1], required_corners[1][1], max_y / resolution, required_delta_y * dpb, 0))
    )

def save_overlay(min_x, min_y, max_x, max_y, canvas, resolution=resolution, dpb=dpb, overlay_path="", DEBUG=False):
    required_corners = expand_corners(min_x, min_y, max_x, max_y, resolution=resolution)

    required_delta_x = abs(required_corners[1][0] - required_corners[0][0])
    required_delta_y = abs(required_corners[1][1] - required_corners[0][1])

    __x =  math.ceil(min_x / resolution) if min_x > 0 else math.floor(min_x / resolution)
    __y =  math.ceil(min_y / resolution) if min_y > 0 else math.floor(min_y / resolution)
    for _x in range(required_delta_x):
        x = _x + __x - __sign(min_x)
        for _y in range(required_delta_y):
            y = _y + __y - __sign(min_y)
            overlay_alias = "{}_{}_{}{}".format(__quad(x, y), abs(y), abs(x), "_sigma_mu")
            if DEBUG: print("Saving as {}".format(overlay_alias))
            overlay_segment = Image.fromarray(canvas[_y*dpb:(_y+1)*dpb, _x*dpb:(_x+1)*dpb, :])
            overlay_segment.save(overlay_path+"graphics/"+overlay_alias+".png", format="PNG")