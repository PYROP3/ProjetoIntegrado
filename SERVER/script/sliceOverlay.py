from PIL import Image
import os
import sys
import argparse
import json

DEBUG = False

def rangeMap(min1, max1, val1, min2, max2):
    return min2 + (val1 - min1) * (max2 - min2)/(max1 - min1)

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
req_x_min = args.x_min[0]
req_x_max = args.x_max[0]
req_y_min = args.y_min[0]
req_y_max = args.y_max[0]

with open(args.overlay_folder[0]+"json/"+args.overlay[0]+".json", "r") as f:
    overlayData = json.load(f)

# Check for bad requests
if (req_x_max <= req_x_min): exit(1)
if (req_y_max <= req_y_min): exit(1)
if (req_x_min < overlayData["minBoundingX"]): exit(1)
if (req_y_min < overlayData["minBoundingY"]): exit(1)
if (req_x_max > overlayData["maxBoundingX"]): exit(1)
if (req_y_max > overlayData["maxBoundingY"]): exit(1)

overlay = Image.open(args.overlay_folder[0]+"graphics/"+args.overlay[0]+".png").convert("RGB");

(left, upper, right, lower) = (
    rangeMap(overlayData["minBoundingX"], overlayData["maxBoundingX"], req_x_min, 0, overlay.size[0]),
    rangeMap(overlayData["minBoundingY"], overlayData["maxBoundingY"], req_y_min, 0, overlay.size[1]),
    rangeMap(overlayData["minBoundingX"], overlayData["maxBoundingX"], req_x_max, 0, overlay.size[0]),
    rangeMap(overlayData["minBoundingY"], overlayData["maxBoundingY"], req_y_max, 0, overlay.size[1])
)

if DEBUG: print("Cropping {}, {}, {}, {}".format(left, upper, right, lower))

overlay = overlay.crop((left, upper, right, lower))

nonce = os.urandom(32).hex()

with open("./tmp/"+nonce+".jpg", "w") as f:
    overlay.save(f, format='JPEG')

print(nonce, end="")

# with io.BytesIO() as output:
#     overlay.save(output, format='PNG')
#     sys.stdout.buffer.write(output.getvalue())