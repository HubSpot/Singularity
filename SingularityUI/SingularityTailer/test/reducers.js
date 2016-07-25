/* eslint-env node, mocha */
import expect from 'expect';
import reducer from '../src/reducers';

import * as types from '../src/actions';

import { List } from 'immutable';


import {
  createMissingMarker,
  splitChunkIntoLines,
  mergeChunks,
  createLines,
  mergeLines
} from '../src/reducers/chunk';

const splitChunkIntoLinesHelper = (chunk) => {
  return splitChunkIntoLines(chunk).toArray();
};

describe('splitChunkIntoLines', () => {
  it('should count the next newlines correctly', () => {
    expect(
      splitChunkIntoLinesHelper({
        text: 'asdf\nasdg\nasdh',
        start: 0,
        end: 14,
        byteLength: 14
      })
    ).toEqual([
      {
        text: 'asdf',
        byteLength: 4,
        start: 0,
        end: 5,
        hasNewline: true
      },
      {
        text: 'asdg',
        byteLength: 4,
        start: 5,
        end: 10,
        hasNewline: true
      },
      {
        text: 'asdh',
        byteLength: 4,
        start: 10,
        end: 14,
        hasNewline: false
      }
    ]);
  });

  it('should count multi-byte characters correctly', () => {
    expect(
      splitChunkIntoLinesHelper({
        text: '\u{1F643}',
        start: 0,
        end: 4,
        length: 4
      })
    ).toEqual([
      {
        text: '\u{1F643}',
        byteLength: 4,
        start: 0,
        end: 4,
        hasNewline: false
      },
    ]);
  });

  it('should handle newlines at the end of chunk', () => {
    expect(
      splitChunkIntoLinesHelper({
        text: '\u{1F643}\n',
        start: 1000,
        end: 1005,
        length: 5
      })
    ).toEqual([
      {
        text: '\u{1F643}',
        byteLength: 4,
        start: 1000,
        end: 1005,
        hasNewline: true
      },
      {
        text: '',
        byteLength: 0,
        start: 1005,
        end: 1005,
        hasNewline: false
      }
    ]);
  });

  it('should handle newlines at the beginning of chunk', () => {
    expect(
      splitChunkIntoLinesHelper({
        text: '\n\u{1F643}',
        start: 1000,
        end: 1005,
        length: 5
      })
    ).toEqual([
      {
        text: '',
        byteLength: 0,
        start: 1000,
        end: 1001,
        hasNewline: true
      },
      {
        text: '\u{1F643}',
        byteLength: 4,
        start: 1001,
        end: 1005,
        hasNewline: false
      }
    ]);
  });
});

const mergeChunksTestHelper = (incoming, existing) => {
  return mergeChunks(incoming, new List(existing)).toArray();
};

describe('mergeChunks', () => {
  it('should be able to add chunks to empty list', () => {
    expect(
      mergeChunksTestHelper(
        {
          text: 'asdf',
          byteLength: 4,
          start: 0,
          end: 4
        },
        []
      )
    ).toEqual(
      [
        {
          text: 'asdf',
          byteLength: 4,
          start: 0,
          end: 4
        }
      ]
    );
  });
  it('should be able to add chunks that don\'t interfere', () => {
    expect(
      mergeChunksTestHelper(
        {
          text: 'asdf',
          byteLength: 4,
          start: 0,
          end: 4
        },
        [
          {
            text: 'hi there',
            byteLength: 8,
            start: 1000,
            end: 1008
          }
        ]
      )
    ).toEqual(
      [
        {
          text: 'asdf',
          byteLength: 4,
          start: 0,
          end: 4
        },
        {
          text: 'hi there',
          byteLength: 8,
          start: 1000,
          end: 1008
        }
      ]
    );

    expect(
      mergeChunksTestHelper(
        {
          text: 'asdf',
          byteLength: 4,
          start: 2000,
          end: 2004
        },
        [
          {
            text: 'hi there',
            byteLength: 8,
            start: 1000,
            end: 1008
          }
        ]
      )
    ).toEqual(
      [
        {
          text: 'hi there',
          byteLength: 8,
          start: 1000,
          end: 1008
        },
        {
          text: 'asdf',
          byteLength: 4,
          start: 2000,
          end: 2004
        }
      ]
    );

    expect(
      mergeChunksTestHelper(
        {
          text: 'asdf',
          byteLength: 4,
          start: 2000,
          end: 2004
        },
        [
          {
            text: 'hi there',
            byteLength: 8,
            start: 1000,
            end: 1008
          },
          {
            text: 'what\'s happening',
            byteLength: 16,
            start: 3000,
            end: 3016
          },
          {
            text: 'hi there',
            byteLength: 8,
            start: 4000,
            end: 4008
          }
        ]
      )
    ).toEqual(
      [
        {
          text: 'hi there',
          byteLength: 8,
          start: 1000,
          end: 1008
        },
        {
          text: 'asdf',
          byteLength: 4,
          start: 2000,
          end: 2004
        },
        {
          text: 'what\'s happening',
          byteLength: 16,
          start: 3000,
          end: 3016
        },
        {
          text: 'hi there',
          byteLength: 8,
          start: 4000,
          end: 4008
        }
      ]
    );
  });
  it('should be able to add a chunk right before another', () => {
    expect(
      mergeChunksTestHelper(
        {
          text: 'asdf',
          byteLength: 4,
          start: 2000,
          end: 2004
        },
        [
          {
            text: 'hi there',
            byteLength: 8,
            start: 2004,
            end: 2012
          }
        ]
      )
    ).toEqual(
      [
        {
          text: 'asdf',
          byteLength: 4,
          start: 2000,
          end: 2004
        },
        {
          text: 'hi there',
          byteLength: 8,
          start: 2004,
          end: 2012
        }
      ]
    );
  });
  it('should be able to add a chunk right after another', () => {
    expect(
      mergeChunksTestHelper(
        {
          text: 'asdf',
          byteLength: 4,
          start: 2008,
          end: 2012
        },
        [
          {
            text: 'hi there',
            byteLength: 8,
            start: 2000,
            end: 2008
          }
        ]
      )
    ).toEqual(
      [
        {
          text: 'hi there',
          byteLength: 8,
          start: 2000,
          end: 2008
        },
        {
          text: 'asdf',
          byteLength: 4,
          start: 2008,
          end: 2012
        }
      ]
    );
  });
  it('should be able to merge a chunk that overlaps the end of another chunk', () => {
    expect(
      mergeChunksTestHelper(
        {
          text: 'great',
          byteLength: 5,
          start: 1021,
          end: 1026
        },
        [
          {
            text: 'these sandwiches are bad',
            byteLength: 24,
            start: 1000,
            end: 1024
          }
        ]
      )
    ).toEqual(
      [
        {
          text: 'these sandwiches are great',
          byteLength: 26,
          start: 1000,
          end: 1026
        }
      ]
    );
  });
  it('should be able to merge a chunk that overlaps the beginning of another chunk', () => {
    expect(
      mergeChunksTestHelper(
        {
          text: 'fact:',
          byteLength: 5,
          start: 1000,
          end: 1005
        },
        [
          {
            text: 'these sandwiches are bad',
            byteLength: 24,
            start: 1000,
            end: 1024
          }
        ]
      )
    ).toEqual(
      [
        {
          text: 'fact: sandwiches are bad',
          byteLength: 24,
          start: 1000,
          end: 1024
        }
      ]
    );
  });
  it('should be able to merge a chunk that is in the middle of another chunk', () => {
    expect(
      mergeChunksTestHelper(
        {
          text: 'butterfish',
          byteLength: 10,
          start: 1006,
          end: 1016
        },
        [
          {
            text: 'these sandwiches are great',
            byteLength: 26,
            start: 1000,
            end: 1026
          }
        ]
      )
    ).toEqual(
      [
        {
          text: 'these butterfish are great',
          byteLength: 26,
          start: 1000,
          end: 1026
        }
      ]
    );
  });
  it('should be able to merge a chunk that fully overlaps multiple chunks', () => {
    expect(
      mergeChunksTestHelper(
        {
          text: '1234567890',
          byteLength: 10,
          start: 1000,
          end: 1010
        },
        [
          {
            text: 'abc',
            byteLength: 3,
            start: 1000,
            end: 1003
          },
          {
            text: 'blb',
            byteLength: 3,
            start: 1006,
            end: 1009
          }
        ]
      )
    ).toEqual(
      [
        {
          text: '1234567890',
          byteLength: 10,
          start: 1000,
          end: 1010
        }
      ]
    );
  });
  it('should be able to merge a chunk that partially overlaps multiple chunks', () => {
    expect(
      mergeChunksTestHelper(
        {
          text: '1234567890',
          byteLength: 10,
          start: 1000,
          end: 1010
        },
        [
          {
            text: 'abc',
            byteLength: 3,
            start: 998,
            end: 1001
          },
          {
            text: 'blb',
            byteLength: 3,
            start: 1006,
            end: 1009
          }
        ]
      )
    ).toEqual(
      [
        {
          text: 'ab1234567890',
          byteLength: 12,
          start: 998,
          end: 1010
        }
      ]
    );

    expect(
      mergeChunksTestHelper(
        {
          text: '1234567890',
          byteLength: 10,
          start: 1000,
          end: 1010
        },
        [
          {
            text: 'abc',
            byteLength: 3,
            start: 1001,
            end: 1004
          },
          {
            text: 'blb',
            byteLength: 3,
            start: 1009,
            end: 1012
          }
        ]
      )
    ).toEqual(
      [
        {
          text: '1234567890lb',
          byteLength: 12,
          start: 1000,
          end: 1012
        }
      ]
    );

    expect(
      mergeChunksTestHelper(
        {
          text: '1234567890',
          byteLength: 10,
          start: 1000,
          end: 1010
        },
        [
          {
            text: 'abc',
            byteLength: 3,
            start: 998,
            end: 1001
          },
          {
            text: 'blb',
            byteLength: 3,
            start: 1009,
            end: 1012
          }
        ]
      )
    ).toEqual(
      [
        {
          text: 'ab1234567890lb',
          byteLength: 14,
          start: 998,
          end: 1012
        }
      ]
    );

    expect(
      mergeChunksTestHelper(
        {
          text: 'and',
          byteLength: 3,
          start: 1008,
          end: 1011
        },
        [
          {
            text: 'waffles  ',
            byteLength: 9,
            start: 1000,
            end: 1009
          },
          {
            text: '  pancakes',
            byteLength: 10,
            start: 1010,
            end: 1020
          }
        ]
      )
    ).toEqual(
      [
        {
          text: 'waffles and pancakes',
          byteLength: 20,
          start: 1000,
          end: 1020
        }
      ]
    );
  });
});

const createLinesHelper = (chunks, range) => {
  return createLines(new List(chunks), range).toArray();
};

describe('createLines', () => {
  it('should work for no input', () => {
    expect(
      createLinesHelper(
        [],
        {
          start: 0,
          end: 0
        }
      )
    ).toEqual(
      []
    );
  });

  describe('single chunk', () => {
    it('should work for the full range', () => {
      expect(
        createLinesHelper(
          [
            {
              text: 'waffles\nand\npancakes',
              byteLength: 20,
              start: 0,
              end: 20
            }
          ],
          {
            start: 0,
            end: 20
          }
        )
      ).toEqual(
        [
          {
            text: 'waffles',
            byteLength: 7,
            start: 0,
            end: 8,
            hasNewline: true
          },
          {
            text: 'and',
            byteLength: 3,
            start: 8,
            end: 12,
            hasNewline: true
          },
          {
            text: 'pancakes',
            byteLength: 8,
            start: 12,
            end: 20,
            hasNewline: false
          }
        ]
      );

      expect(
        createLinesHelper(
          [
            {
              text: 'waffles\nand\npancakes',
              byteLength: 20,
              start: 1000,
              end: 1020
            }
          ],
          {
            start: 1000,
            end: 1020
          }
        )
      ).toEqual(
        [
          {
            text: 'waffles',
            byteLength: 7,
            start: 1000,
            end: 1008,
            hasNewline: true
          },
          {
            text: 'and',
            byteLength: 3,
            start: 1008,
            end: 1012,
            hasNewline: true
          },
          {
            text: 'pancakes',
            byteLength: 8,
            start: 1012,
            end: 1020,
            hasNewline: false
          }
        ]
      );
    });

    it('should work for a partial range', () => {
      expect(
        createLinesHelper(
          [
            {
              text: 'waffles\nand\npancakes',
              byteLength: 20,
              start: 0,
              end: 20
            }
          ],
          {
            start: 0,
            end: 10
          }
        )
      ).toEqual(
        [
          {
            text: 'waffles',
            byteLength: 7,
            start: 0,
            end: 8,
            hasNewline: true
          },
          {
            text: 'and',
            byteLength: 3,
            start: 8,
            end: 12,
            hasNewline: true
          }
        ]
      );

      expect(
        createLinesHelper(
          [
            {
              text: 'waffles\nand\npancakes',
              byteLength: 20,
              start: 0,
              end: 20
            }
          ],
          {
            start: 10,
            end: 20
          }
        )
      ).toEqual(
        [
          {
            text: 'and',
            byteLength: 3,
            start: 8,
            end: 12,
            hasNewline: true
          },
          {
            text: 'pancakes',
            byteLength: 8,
            start: 12,
            end: 20,
            hasNewline: false
          }
        ]
      );
    });
  });

  describe('multiple chunks', () => {
    it('should work for the full range with chunks that have no gap', () => {
      expect(
        createLinesHelper(
          [
            {
              text: 'waffles\nand\npancakes',
              byteLength: 20,
              start: 1000,
              end: 1020
            },
            {
              text: 'waffles\nand\npancakes',
              byteLength: 20,
              start: 1020,
              end: 1040
            }
          ],
          {
            start: 1000,
            end: 1040
          }
        )
      ).toEqual(
        [
          {
            text: 'waffles',
            byteLength: 7,
            start: 1000,
            end: 1008,
            hasNewline: true
          },
          {
            text: 'and',
            byteLength: 3,
            start: 1008,
            end: 1012,
            hasNewline: true
          },
          {
            text: 'pancakeswaffles',
            byteLength: 15,
            start: 1012,
            end: 1028,
            hasNewline: true
          },
          {
            text: 'and',
            byteLength: 3,
            start: 1028,
            end: 1032,
            hasNewline: true
          },
          {
            text: 'pancakes',
            byteLength: 8,
            start: 1032,
            end: 1040,
            hasNewline: false
          }
        ]
      );
    });

    it('should work for the full range with chunks that have a gap', () => {
      expect(
        createLinesHelper(
          [
            {
              text: 'waffles\nand\npancakes',
              byteLength: 20,
              start: 1000,
              end: 1020
            },
            {
              text: 'waffles\nand\npancakes',
              byteLength: 20,
              start: 2000,
              end: 2020
            }
          ],
          {
            start: 1000,
            end: 2020
          }
        )
      ).toEqual(
        [
          {
            text: 'waffles',
            byteLength: 7,
            start: 1000,
            end: 1008,
            hasNewline: true
          },
          {
            text: 'and',
            byteLength: 3,
            start: 1008,
            end: 1012,
            hasNewline: true
          },
          {
            text: 'pancakes',
            byteLength: 8,
            start: 1012,
            end: 1020,
            hasNewline: false
          },
          createMissingMarker(1020, 2000),
          {
            text: 'waffles',
            byteLength: 7,
            start: 2000,
            end: 2008,
            hasNewline: true
          },
          {
            text: 'and',
            byteLength: 3,
            start: 2008,
            end: 2012,
            hasNewline: true
          },
          {
            text: 'pancakes',
            byteLength: 8,
            start: 2012,
            end: 2020,
            hasNewline: false
          }
        ]
      );
    });

    it('should work for the partial range with chunks that have no gap', () => {
      expect(
        createLinesHelper(
          [
            {
              text: 'waffles\nand\npancakes',
              byteLength: 20,
              start: 1000,
              end: 1020
            },
            {
              text: 'waffles\nand\npancakes',
              byteLength: 20,
              start: 1020,
              end: 1040
            }
          ],
          {
            start: 1010,
            end: 1030
          }
        )
      ).toEqual(
        [
          {
            text: 'and',
            byteLength: 3,
            start: 1008,
            end: 1012,
            hasNewline: true
          },
          {
            text: 'pancakeswaffles',
            byteLength: 15,
            start: 1012,
            end: 1028,
            hasNewline: true
          },
          {
            text: 'and',
            byteLength: 3,
            start: 1028,
            end: 1032,
            hasNewline: true
          }
        ]
      );
    });
    it('should work for the partial range with chunks that have a gap', () => {
      expect(
        createLinesHelper(
          [
            {
              text: 'waffles\nand\npancakes',
              byteLength: 20,
              start: 1000,
              end: 1020
            },
            {
              text: 'waffles\nand\npancakes',
              byteLength: 20,
              start: 2000,
              end: 2020
            }
          ],
          {
            start: 1015,
            end: 2001
          }
        )
      ).toEqual(
        [
          {
            text: 'pancakes',
            byteLength: 8,
            start: 1012,
            end: 1020,
            hasNewline: false
          },
          createMissingMarker(1020, 2000),
          {
            text: 'waffles',
            byteLength: 7,
            start: 2000,
            end: 2008,
            hasNewline: true
          }
        ]
      );
    });
  });
});

const mergeLinesHelper = (incoming, existing) => {
  return mergeLines(new List(incoming), new List(existing)).toArray();
};

describe('mergeLines', () => {
  it('should be able to merge lines with empty'); // maybe not?
  it('should be able to merge lines to the beginning');
  it('should be able to merge lines at the end');
  it('should be able to merge lines in the middle');
  it('should be able to replace lines in the middle');
});

describe('addChunkReducer', () => {
  it('should be able to initialize an empty log');
  describe('should be able to add a chunk to an existing log', () => {
    it('before the existing data');
    it('after the existing data');
    it('in between the existing data');
  });
});
