# !/usr/bin/python3
# %%
import sys
import os

import numpy as np
import mkv
import matplotlib.pyplot as plt
# This is for tex plots according to acm format
from datetime import datetime



FILENAME = "measure.mkv" # start 13.52 end 17.02
TITLE_TO_LOAD = "SmartDevice" # "SmartDevice" for own test
# %%
myInfo = mkv.info(FILENAME)
print("myInfo: ")
print(myInfo)
print([s["title"] for s in myInfo["streams"]])
try:
    stream = next(s["streamIndex"] for s in myInfo["streams"] if s["title"] == TITLE_TO_LOAD)
except StopIteration:
    sys.exit("cannot find stream") 

dataDict = mkv.load(FILENAME, streamsToLoad=[stream])[0]

print(dataDict)

# %%
start = float(0) # float(dataDict["metadata"]["TIMESTAMP"])
end = start+(len(dataDict["data"])/dataDict["samplingrate"])
timestamps = np.linspace(start, end, len(dataDict["data"]))
dates = [datetime.fromtimestamp(ts) for ts in timestamps]
# Plot data
fig, ax = plt.subplots()
# ax.plot(dates, dataDict["data"]["v"], label="voltage")
ax.plot(dates, dataDict["data"]["i"], label="current")
# ax.plot(dates, dataDict["data"]["p"], label="active power")
# ax.plot(dates, dataDict["data"]["q"], label="reactive power")
# Format plot
# ax.set(xlabel='Time of day', ylabel='Power [W/var]', title='Laptop Charger')
ax.set(xlabel='Time of day', ylabel='Voltage [V/mA]', title='Laptop Charger')
fig.autofmt_xdate()
plt.show()

# %%

# %%
