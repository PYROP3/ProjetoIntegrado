#!/usr/bin/env python
# coding: utf-8

# In[1]:


import numpy as np
import matplotlib.pyplot as plt
import re


# In[2]:


match_txt = "x: ([-0-9\.]*) y: ([-0-9\.]*) z: ([-0-9\.]*)"


# In[35]:


xs = []
ys = []
zs = []
with open("fastest.txt", "r", encoding="utf8") as f:
    for line in f:
        stripped_line = line.strip()
        xyz = re.search(match_txt, stripped_line)
        try:
            x, y, z = xyz.group(1), xyz.group(2), xyz.group(3)
            #print("X = {}, Y = {}, Z = {}".format(x, y, z))
            xs.append(float(x))
            ys.append(float(y))
            zs.append(float(z))
        except:
            pass


# In[ ]:


print(str(xs))


# In[36]:


ts = np.arange(0, len(xs), 1)
print(str(ts))


# In[37]:


plt.clf()
fig, (ax1, ax2, ax3) = plt.subplots(nrows=3, ncols=1, figsize=(40,30))
ax1.plot(ts, xs, color='red')
ax2.plot(ts, ys, color='green')
ax3.plot(ts, zs, color='blue')
plt.show()


# In[ ]:




