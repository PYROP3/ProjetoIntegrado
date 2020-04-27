import json

class ErrorHandler:
    def _add(self, errorName, errorCode=None):
        assert errorName not in self.Errors, "Found duplicate error {} (previous ID: {})".format(errorName, self.Errors[errorName])
        self.Errors[errorName] = errorCode or self._last
        self._last = self.Errors[errorName] + 1

    def __init__(self, errorCodesPath):
        self.Errors = {}
        self._last = 2

        with open(errorCodesPath, 'r') as f:
            errorList = json.load(f)
            for errorD in errorList:
                self._add(errorD["Name"], errorD["id"])

    def exitOnError(self, errorName):
        if errorName not in self.Errors:
            exit(self.Errors["UnknownError"])
        exit(self.Errors[errorName])