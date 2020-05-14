class GradientError(RuntimeError):
    """Catch-all exception for """
    pass

class interpolationGradient:
    def __init__(self, colors=[], points=[]):
        assert len(colors) == len(points), "Lengths differ: {} x {}".format(len(colors), len(points))
        self._colors = [(points[i], colors[i]) for i, c in enumerate(colors)]
        self._sorted = False

    def _interpolate(self, min1, max1, val1, min2, max2):
        return min2 + (val1 - min1) * (max2 - min2)/(max1 - min1)

    def add(self, color, point):
        if point in [t[0] for t in self._colors]:
            raise GradientError("Point {} already in list".format(point))
        self._colors.append((point, color))
        self._sorted = False

    def sort(self):
        assert len(self._colors) > 0, "Add at least one point to gradient"
        self._colors.sort(key=lambda x: x[0])
        self._sorted = True

    def colorize(self, point):
        assert len(self._colors) > 0, "Add at least one point to gradient"
        assert self._sorted, "Sort gradient before colorizing"
        if point <= self._colors[0][0]:
            return self._colors[0][1]
        if point >= self._colors[-1][0]:
            return self._colors[-1][1]
        if point in [t[0] for t in self._colors]:
            return self._colors[[t[0] for t in self._colors].index(point)][1]
        aux = self._colors + [(point, 0)]
        aux.sort(key=lambda x: x[0])
        auxInd = [t[0] for t in aux].index(point)
        minP = self._colors[auxInd - 1]
        maxP = self._colors[auxInd]
        assert len(minP[1]) == len(maxP[1]), "Found different color channels: {} vs {}".format(len(minP[1]), len(maxP[1]))
        return [self._interpolate(minP[0], maxP[0], point, minP[1][i], maxP[1][i]) for i, p in enumerate(minP[1])]

    def colorizeChannel(self, point, channel=0):
        return self.colorize(point)[channel]

    def colorizeR(self, point):
        return self.colorize(point)[0]

    def colorizeG(self, point):
        return self.colorize(point)[1]

    def colorizeB(self, point):
        return self.colorize(point)[2]

    def colorizeArray(self, points):
        return map(self.colorize, points)

# from gradients import interpolationGradient
# G = interpolationGradient()
# G.add([235, 64, 52], 0)
# G.add([235, 140, 52], 0.33)
# G.add([235, 223, 52], 0.67)
# G.add([162, 235, 52], 1)
# G.sort()