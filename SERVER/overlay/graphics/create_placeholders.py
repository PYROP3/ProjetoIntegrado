from PIL import Image, ImageDraw, ImageFont

k = 1
fnt = ImageFont.truetype('arial.ttf', 40)

source_mode = True

lx = 4696
ly = 2280
ux = 4722
uy = 2307

side = 500

for x in range(ux-lx+1):
    for y in range(uy-ly+1):
        txt = "SW_{}_{}{}".format(x+lx, y+ly, ("_source" if source_mode else "_sigma_mu"))
        if source_mode: # Source mode (for testing cropping)
            img = Image.new("RGB", (side, side), ((x+y)%2*255, 0, (x+y)%2*255))
            d = ImageDraw.Draw(img)
            d.text((20, 20), txt, font=fnt, fill=(127, 127, 127, 255))
        else: # Sigma-mu mode (for testing maths)
            img = Image.new("RGB", (side, side), (127, 255, 0))
        img.save(txt+".png", format="PNG")