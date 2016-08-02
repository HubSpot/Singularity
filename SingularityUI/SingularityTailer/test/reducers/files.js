/* eslint-env node, mocha */
import expect from 'expect';

import * as Actions from '../../src/actions';

import { List } from 'immutable';

import {
  createMissingMarker,
  splitChunkIntoLines,
  mergeChunks,
  createLines,
  mergeLines,
  addChunkReducer
} from '../../src/reducers/files';

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

const mergeChunksTestHelper = (existing, incoming) => {
  return mergeChunks(new List(existing), incoming).toArray();
};

describe('mergeChunks', () => {
  it('should be able to add chunks to empty list', () => {
    expect(
      mergeChunksTestHelper(
        [],
        {
          text: 'asdf',
          byteLength: 4,
          start: 0,
          end: 4
        }
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
        [
          {
            text: 'hi there',
            byteLength: 8,
            start: 1000,
            end: 1008
          }
        ],
        {
          text: 'asdf',
          byteLength: 4,
          start: 0,
          end: 4
        }
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
        [
          {
            text: 'hi there',
            byteLength: 8,
            start: 1000,
            end: 1008
          }
        ],
        {
          text: 'asdf',
          byteLength: 4,
          start: 2000,
          end: 2004
        }
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
        ],
        {
          text: 'asdf',
          byteLength: 4,
          start: 2000,
          end: 2004
        }
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
        [
          {
            text: 'hi there',
            byteLength: 8,
            start: 2004,
            end: 2012
          }
        ],
        {
          text: 'asdf',
          byteLength: 4,
          start: 2000,
          end: 2004
        }
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

    expect(
      mergeChunksTestHelper(
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
        ],
        {
          text: 'characters',
          byteLength: 10,
          start: 1990,
          end: 2000
        }
      )
    ).toEqual(
      [
        {
          text: 'characters',
          byteLength: 10,
          start: 1990,
          end: 2000
        },
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
        [
          {
            text: 'hi there',
            byteLength: 8,
            start: 2000,
            end: 2008
          }
        ],
        {
          text: 'asdf',
          byteLength: 4,
          start: 2008,
          end: 2012
        }
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

    expect(
      mergeChunksTestHelper(
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
        ],
        {
          text: '1234',
          byteLength: 4,
          start: 2012,
          end: 2016
        }
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
        },
        {
          text: '1234',
          byteLength: 4,
          start: 2012,
          end: 2016
        }
      ]
    );
  });

  it('should be able to add a chunk with a space after another', () => {
    expect(
      mergeChunksTestHelper(
        [
          {
            text: 'hi there',
            byteLength: 8,
            start: 2000,
            end: 2008
          }
        ],
        {
          text: 'asdf',
          byteLength: 4,
          start: 2009,
          end: 2013
        }
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
          start: 2009,
          end: 2013
        }
      ]
    );

    expect(
      mergeChunksTestHelper(
        [
          {
            text: 'hi there',
            byteLength: 8,
            start: 2000,
            end: 2008
          },
          {
            text: 'password',
            byteLength: 8,
            start: 2008,
            end: 2016
          }
        ],
        {
          text: 'asdf',
          byteLength: 4,
          start: 2017,
          end: 2021
        }
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
          text: 'password',
          byteLength: 8,
          start: 2008,
          end: 2016
        },
        {
          text: 'asdf',
          byteLength: 4,
          start: 2017,
          end: 2021
        }
      ]
    );
  });

  it('should be able to merge a chunk that overlaps the end of another chunk', () => {
    expect(
      mergeChunksTestHelper(
        [
          {
            text: 'these sandwiches are bad',
            byteLength: 24,
            start: 1000,
            end: 1024
          }
        ],
        {
          text: 'great',
          byteLength: 5,
          start: 1021,
          end: 1026
        }
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
        [
          {
            text: 'these sandwiches are bad',
            byteLength: 24,
            start: 1000,
            end: 1024
          }
        ],
        {
          text: 'fact:',
          byteLength: 5,
          start: 1000,
          end: 1005
        }
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
        [
          {
            text: 'these sandwiches are great',
            byteLength: 26,
            start: 1000,
            end: 1026
          }
        ],
        {
          text: 'butterfish',
          byteLength: 10,
          start: 1006,
          end: 1016
        }
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
        ],
        {
          text: '1234567890',
          byteLength: 10,
          start: 1000,
          end: 1010
        }
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
        ],
        {
          text: '1234567890',
          byteLength: 10,
          start: 1000,
          end: 1010
        }
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
        ],
        {
          text: '1234567890',
          byteLength: 10,
          start: 1000,
          end: 1010
        }
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
        ],
        {
          text: '1234567890',
          byteLength: 10,
          start: 1000,
          end: 1010
        }
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
        ],
        {
          text: 'and',
          byteLength: 3,
          start: 1008,
          end: 1011
        }
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

const createLinesHelper = (chunks) => {
  return createLines(new List(chunks)).toArray();
};

describe('createLines', () => {
  it('should work for no input', () => {
    expect(
      createLinesHelper(
        []
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
          ]
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
          ]
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
          ]
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
          ]
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
  });
});

const mergeLinesHelper = (existing, incoming, replacementRange) => {
  return mergeLines(
    new List(existing),
    new List(incoming),
    replacementRange
  ).toArray();
};

describe('mergeLines', () => {
  it('should be able to merge lines to the beginning', () => {
    expect(
      mergeLinesHelper(
        [
          createMissingMarker(0, 35),
          {
            text: 'a line that has info',
            byteLength: 20,
            start: 35,
            end: 56,
            hasNewline: true
          }
        ],
        [
          {
            text: 'line at the beginning',
            byteLength: 21,
            start: 0,
            end: 22,
            hasNewline: true
          },
          {
            text: 'another line',
            byteLength: 12,
            start: 22,
            end: 35,
            hasNewline: true
          }
        ],
        {
          startIndex: 0,
          endIndex: 0
        }
      )
    ).toEqual(
      [
        {
          text: 'line at the beginning',
          byteLength: 21,
          start: 0,
          end: 22,
          hasNewline: true
        },
        {
          text: 'another line',
          byteLength: 12,
          start: 22,
          end: 35,
          hasNewline: true
        },
        {
          text: 'a line that has info',
          byteLength: 20,
          start: 35,
          end: 56,
          hasNewline: true
        }
      ]
    );

    expect(
      mergeLinesHelper(
        [
          createMissingMarker(0, 135),
          {
            text: 'a line that has info',
            byteLength: 20,
            start: 135,
            end: 156,
            hasNewline: true
          }
        ],
        [
          {
            text: 'line at the beginning',
            byteLength: 21,
            start: 0,
            end: 22,
            hasNewline: true
          },
          {
            text: 'another line',
            byteLength: 12,
            start: 22,
            end: 35,
            hasNewline: true
          }
        ],
        {
          startIndex: 0,
          endIndex: 0
        }
      )
    ).toEqual(
      [
        {
          text: 'line at the beginning',
          byteLength: 21,
          start: 0,
          end: 22,
          hasNewline: true
        },
        {
          text: 'another line',
          byteLength: 12,
          start: 22,
          end: 35,
          hasNewline: true
        },
        createMissingMarker(35, 135),
        {
          text: 'a line that has info',
          byteLength: 20,
          start: 135,
          end: 156,
          hasNewline: true
        }
      ]
    );
  });

  it('should be able to merge lines at the end', () => {
    expect(
      mergeLinesHelper(
        [
          {
            text: 'line at the beginning',
            byteLength: 21,
            start: 0,
            end: 22,
            hasNewline: true
          },
          {
            text: 'another line',
            byteLength: 12,
            start: 22,
            end: 35,
            hasNewline: true
          }
        ],
        [
          {
            text: 'another line',
            byteLength: 12,
            start: 22,
            end: 35,
            hasNewline: true
          },
          {
            text: 'a line that has info',
            byteLength: 20,
            start: 35,
            end: 56,
            hasNewline: true
          }
        ],
        {
          startIndex: 1,
          endIndex: 1
        }
      )
    ).toEqual(
      [
        {
          text: 'line at the beginning',
          byteLength: 21,
          start: 0,
          end: 22,
          hasNewline: true
        },
        {
          text: 'another line',
          byteLength: 12,
          start: 22,
          end: 35,
          hasNewline: true
        },
        {
          text: 'a line that has info',
          byteLength: 20,
          start: 35,
          end: 56,
          hasNewline: true
        }
      ]
    );
  });

  it('should be able to merge lines in the middle');
  it('should be able to replace lines in the middle');
});

const addChunkReducerHelper = (state, action) => {
  const modifiedState = {};
  for (const key of Object.keys(state)) {
    modifiedState[key] = {
      chunks: new List(state[key].chunks),
      lines: new List(state[key].lines),
      fileSize: state[key].fileSize
    };
  }

  const modifiedReturn = {};
  const returned = addChunkReducer(modifiedState, action);
  for (const key of Object.keys(returned)) {
    modifiedReturn[key] = {
      chunks: returned[key].chunks.toArray(),
      lines: returned[key].lines.toArray(),
      fileSize: returned[key].fileSize
    };
  }

  return modifiedReturn;
};

describe('addChunkReducer', () => {
  it('should be able to initialize an empty log', () => {
    expect(
      addChunkReducerHelper({},
        Actions.addFileChunk('test', {
          text: 'should be able to initialize an empty log',
          byteLength: 41,
          start: 0,
          end: 41
        })
      )
    ).toEqual({
      'test': {
        chunks: [
          {
            text: 'should be able to initialize an empty log',
            byteLength: 41,
            start: 0,
            end: 41
          }
        ],
        lines: [
          {
            text: 'should be able to initialize an empty log',
            byteLength: 41,
            start: 0,
            end: 41,
            hasNewline: false
          }
        ],
        fileSize: 41
      }
    });
  });

  describe('should be able to add a chunk to an existing log', () => {
    it('before the existing data', () => {
      expect(
        addChunkReducerHelper(
          {
            'test': {
              chunks: [
                {
                  text: 'non-empty log\nsup',
                  byteLength: 17,
                  start: 30,
                  end: 47
                }
              ],
              lines: [
                createMissingMarker(0, 30),
                {
                  text: 'non-empty log',
                  byteLength: 13,
                  start: 30,
                  end: 44,
                  hasNewline: true
                },
                {
                  text: 'sup',
                  byteLength: 3,
                  start: 44,
                  end: 47,
                  hasNewline: false
                }
              ],
              fileSize: 47
            }
          },
          Actions.addFileChunk('test', {
            text: 'thirty characters right here \n',
            byteLength: 30,
            start: 0,
            end: 30
          })
        )
      ).toEqual(
        {
          'test': {
            chunks: [
              {
                text: 'thirty characters right here \n',
                byteLength: 30,
                start: 0,
                end: 30
              },
              {
                text: 'non-empty log\nsup',
                byteLength: 17,
                start: 30,
                end: 47
              }
            ],
            lines: [
              {
                text: 'thirty characters right here ',
                byteLength: 29,
                start: 0,
                end: 30,
                hasNewline: true
              },
              {
                text: 'non-empty log',
                byteLength: 13,
                start: 30,
                end: 44,
                hasNewline: true
              },
              {
                text: 'sup',
                byteLength: 3,
                start: 44,
                end: 47,
                hasNewline: false
              }
            ],
            fileSize: 47
          }
        }
      );

      expect(
        addChunkReducerHelper(
          {
            'test': {
              chunks: [
                {
                  text: 'non-empty log\nsup',
                  byteLength: 17,
                  start: 30,
                  end: 47
                }
              ],
              lines: [
                createMissingMarker(0, 30),
                {
                  text: 'non-empty log',
                  byteLength: 13,
                  start: 30,
                  end: 44,
                  hasNewline: true
                },
                {
                  text: 'sup',
                  byteLength: 3,
                  start: 44,
                  end: 47,
                  hasNewline: false
                }
              ],
              fileSize: 47
            }
          },
          Actions.addFileChunk('test', {
            text: 'thirty characters right here k',
            byteLength: 30,
            start: 0,
            end: 30
          })
        )
      ).toEqual(
        {
          'test': {
            chunks: [
              {
                text: 'thirty characters right here k',
                byteLength: 30,
                start: 0,
                end: 30
              },
              {
                text: 'non-empty log\nsup',
                byteLength: 17,
                start: 30,
                end: 47
              }
            ],
            lines: [
              {
                text: 'thirty characters right here knon-empty log',
                byteLength: 43,
                start: 0,
                end: 44,
                hasNewline: true
              },
              {
                text: 'sup',
                byteLength: 3,
                start: 44,
                end: 47,
                hasNewline: false
              }
            ],
            fileSize: 47
          }
        }
      );
    });

    it('after the existing data', () => {
      expect(
        addChunkReducerHelper(
          {
            'test': {
              chunks: [
                {
                  text: 'non-empty log',
                  byteLength: 13,
                  start: 0,
                  end: 13
                }
              ],
              lines: [
                {
                  text: 'non-empty log',
                  byteLength: 13,
                  start: 0,
                  end: 13,
                  hasNewline: false
                }
              ],
              fileSize: 13
            }
          },
          Actions.addFileChunk('test', {
            text: ' and some more text',
            byteLength: 19,
            start: 13,
            end: 32
          })
        )
      ).toEqual(
        {
          'test': {
            chunks: [
              {
                text: 'non-empty log',
                byteLength: 13,
                start: 0,
                end: 13
              },
              {
                text: ' and some more text',
                byteLength: 19,
                start: 13,
                end: 32
              }
            ],
            lines: [
              {
                text: 'non-empty log and some more text',
                byteLength: 32,
                start: 0,
                end: 32,
                hasNewline: false
              }
            ],
            fileSize: 32
          }
        }
      );
    });

    it('in between the existing data', () => {
      expect(
        addChunkReducerHelper(
          {
            'test': {
              chunks: [
                {
                  text: 'non-empty log',
                  byteLength: 13,
                  start: 0,
                  end: 13
                },
                {
                  text: 'far away chunk',
                  byteLength: 14,
                  start: 1000,
                  end: 1014
                }
              ],
              lines: [
                {
                  text: 'non-empty log',
                  byteLength: 13,
                  start: 0,
                  end: 13,
                  hasNewline: false
                },
                createMissingMarker(13, 1000),
                {
                  text: 'far away chunk',
                  byteLength: 14,
                  start: 1000,
                  end: 1014,
                  hasNewline: false
                }
              ],
              fileSize: 1014
            }
          },
          Actions.addFileChunk('test', {
            text: ' and some more text',
            byteLength: 19,
            start: 503,
            end: 522
          })
        )
      ).toEqual(
        {
          'test': {
            chunks: [
              {
                text: 'non-empty log',
                byteLength: 13,
                start: 0,
                end: 13
              },
              {
                text: ' and some more text',
                byteLength: 19,
                start: 503,
                end: 522
              },
              {
                text: 'far away chunk',
                byteLength: 14,
                start: 1000,
                end: 1014
              }
            ],
            lines: [
              {
                text: 'non-empty log',
                byteLength: 13,
                start: 0,
                end: 13,
                hasNewline: false
              },
              createMissingMarker(13, 503),
              {
                text: ' and some more text',
                byteLength: 19,
                start: 503,
                end: 522,
                hasNewline: false
              },
              createMissingMarker(522, 1000),
              {
                text: 'far away chunk',
                byteLength: 14,
                start: 1000,
                end: 1014,
                hasNewline: false
              }
            ],
            fileSize: 1014
          }
        }
      );

      expect(
        addChunkReducerHelper(
          {
            'test': {
              chunks: [
                {
                  text: 'non-empty log',
                  byteLength: 13,
                  start: 0,
                  end: 13
                },
                {
                  text: 'far away chunk',
                  byteLength: 14,
                  start: 1000,
                  end: 1014
                }
              ],
              lines: [
                {
                  text: 'non-empty log',
                  byteLength: 13,
                  start: 0,
                  end: 13,
                  hasNewline: false
                },
                createMissingMarker(13, 1000),
                {
                  text: 'far away chunk',
                  byteLength: 14,
                  start: 1000,
                  end: 1014,
                  hasNewline: false
                }
              ],
              fileSize: 1014
            }
          },
          Actions.addFileChunk('test', {
            text: ' and some more text',
            byteLength: 19,
            start: 13,
            end: 32
          })
        )
      ).toEqual(
        {
          'test': {
            chunks: [
              {
                text: 'non-empty log',
                byteLength: 13,
                start: 0,
                end: 13
              },
              {
                text: ' and some more text',
                byteLength: 19,
                start: 13,
                end: 32
              },
              {
                text: 'far away chunk',
                byteLength: 14,
                start: 1000,
                end: 1014
              }
            ],
            lines: [
              {
                text: 'non-empty log and some more text',
                byteLength: 32,
                start: 0,
                end: 32,
                hasNewline: false
              },
              createMissingMarker(32, 1000),
              {
                text: 'far away chunk',
                byteLength: 14,
                start: 1000,
                end: 1014,
                hasNewline: false
              }
            ],
            fileSize: 1014
          }
        }
      );

      expect(
        addChunkReducerHelper(
          {
            'test': {
              chunks: [
                {
                  text: 'non-empty log',
                  byteLength: 13,
                  start: 0,
                  end: 13
                },
                {
                  text: 'far away chunk',
                  byteLength: 14,
                  start: 1019,
                  end: 1033
                }
              ],
              lines: [
                {
                  text: 'non-empty log',
                  byteLength: 13,
                  start: 0,
                  end: 13,
                  hasNewline: false
                },
                createMissingMarker(13, 1019),
                {
                  text: 'far away chunk',
                  byteLength: 14,
                  start: 1019,
                  end: 1033,
                  hasNewline: false
                }
              ],
              fileSize: 1033
            }
          },
          Actions.addFileChunk('test', {
            text: 'and some more text ',
            byteLength: 19,
            start: 1000,
            end: 1019
          })
        )
      ).toEqual(
        {
          'test': {
            chunks: [
              {
                text: 'non-empty log',
                byteLength: 13,
                start: 0,
                end: 13
              },
              {
                text: 'and some more text ',
                byteLength: 19,
                start: 1000,
                end: 1019
              },
              {
                text: 'far away chunk',
                byteLength: 14,
                start: 1019,
                end: 1033
              }
            ],
            lines: [
              {
                text: 'non-empty log',
                byteLength: 13,
                start: 0,
                end: 13,
                hasNewline: false
              },
              createMissingMarker(13, 1000),
              {
                text: 'and some more text far away chunk',
                byteLength: 33,
                start: 1000,
                end: 1033,
                hasNewline: false
              }
            ],
            fileSize: 1033
          }
        }
      );
    });

    it('should handle new lines that are beyond the known eof', () => {
      expect(
        addChunkReducerHelper({
          'tail': {
            'chunks': [
              {
                'text': 'hit',
                'start': 0,
                'end': 3,
                'byteLength': 3
              }
            ],
            'lines': [
              {
                'text': 'hit',
                'byteLength': 3,
                'start': 0,
                'end': 3,
                'hasNewline': false
              }
            ],
            'fileSize': 3
          }
        },
        Actions.addFileChunk('tail', {
          'text': 'lol',
          'start': 10,
          'end': 13,
          'byteLength': 3
        }))
      ).toEqual({
        'tail': {
          'chunks': [
            {
              'text': 'hit',
              'start': 0,
              'end': 3,
              'byteLength': 3
            },
            {
              'text': 'lol',
              'start': 10,
              'end': 13,
              'byteLength': 3
            }
          ],
          'lines': [
            {
              'text': 'hit',
              'byteLength': 3,
              'start': 0,
              'end': 3,
              'hasNewline': false
            },
            {
              'isMissingMarker': true,
              'byteLength': 7,
              'start': 3,
              'end': 10,
              'hasNewline': false
            },
            {
              'text': 'lol',
              'byteLength': 3,
              'start': 10,
              'end': 13,
              'hasNewline': false
            }
          ],
          'fileSize': 13
        }
      });
    });

    it('should be able to handle adding data with zero character lines', () => {
      expect(
        addChunkReducerHelper(
          addChunkReducerHelper(
            {},
            Actions.addFileChunk('tail', {
              'text': 'what\nhit\n',
              'start': 0,
              'end': 9,
              'byteLength': 9
            })
          ),
          Actions.addFileChunk('tail', {
            'text': 'new line\n',
            'start': 9,
            'end': 18,
            'byteLength': 9
          })
        )
      ).toEqual({
        'tail': {
          'chunks': [
            {
              'text': 'what\nhit\n',
              'start': 0,
              'end': 9,
              'byteLength': 9
            },
            {
              'text': 'new line\n',
              'start': 9,
              'end': 18,
              'byteLength': 9
            }
          ],
          'lines': [
            {
              'text': 'what',
              'byteLength': 4,
              'start': 0,
              'end': 5,
              'hasNewline': true
            },
            {
              'text': 'hit',
              'byteLength': 3,
              'start': 5,
              'end': 9,
              'hasNewline': true
            },
            {
              'text': 'new line',
              'byteLength': 8,
              'start': 9,
              'end': 18,
              'hasNewline': true
            },
          ],
          'fileSize': 18
        }
      });
    });
  });
});
