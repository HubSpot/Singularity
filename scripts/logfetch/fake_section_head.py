class FakeSectionHead(object):
    def __init__(self, fp):
        self.fp = fp
        self.sechead = '[Defaults]\n'

    def readline(self):
        if self.sechead:
            try: return self.sechead
            finally: self.sechead = None
        else: return self.fp.readline()

