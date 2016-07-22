/* eslint-env node, mocha */
import expect from 'expect';
import reducer from '../src/reducers';

import * as types from '../src/actions';


import {
  splitChunkIntoLines,
  mergeChunks,
  combineSingleLine
} from '../src/reducers/chunk';

describe('chunk splitter helper', () => {
  it('should count the next newlines correctly', () => {
    expect(
      splitChunkIntoLines({
        data: 'asdf\nasdg\nasdh',
        offset: 0,
        length: 14
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
      splitChunkIntoLines({
        data: '\u{1F643}',
        offset: 0,
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
      splitChunkIntoLines({
        data: '\u{1F643}\n',
        offset: 1000,
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
      splitChunkIntoLines({
        data: '\n\u{1F643}',
        offset: 1000,
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

describe('partial line combiner', () => {
  it('should create markers for areas before and after');

  it('should create markers for area after if starting from beginning');

  it('should create markers for areas before and after even if no data returned');
});

describe('mergeChunks', () => {
  it('should be able to add chunks to empty list', () => {
    expect(
      mergeChunks(
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
      mergeChunks(
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
      mergeChunks(
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
  });
  it('should be able to add a chunk right before another', () => {
    expect(
      mergeChunks(
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
      mergeChunks(
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
      mergeChunks(
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
      mergeChunks(
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
      mergeChunks(
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
      mergeChunks(
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
      mergeChunks(
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
      mergeChunks(
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
      mergeChunks(
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
  });
});

describe('combineSingleLine helper', () => {
  describe('with new text at beginning', () => {
    it('should replace existing if the new encompasses it', () => {
      expect(
        combineSingleLine(
          {
            text: 'asdf',
            byteLength: 4,
            start: 1000,
            end: 1005,
            hasNewline: true
          },
          {
            text: 'as',
            byteLength: 2,
            start: 1000,
            end: 1002,
            hasNewline: false
          }
        )
      ).toEqual(
        [
          {
            text: 'asdf',
            byteLength: 4,
            start: 1000,
            end: 1005,
            hasNewline: true
          }
        ]
      );
    });

    it('should replace part of existing if the new is shorter', () => {
      expect(
        combineSingleLine(
          {
            text: 'abcd',
            byteLength: 4,
            start: 1000,
            end: 1005,
            hasNewline: true
          },
          {
            text: 'zq',
            byteLength: 2,
            start: 1000,
            end: 1002,
            hasNewline: false
          }
        )
      ).toEqual(
        [
          {
            text: 'zqcd',
            byteLength: 4,
            start: 1000,
            end: 1005,
            hasNewline: true
          }
        ]
      );
    });

    it('should replace existing marker if the new encompasses it', () => {
      expect(
        combineSingleLine(
          {
            isMissingMarker: true,
            byteLength: 4,
            start: 1000,
            end: 1004,
            hasNewline: false
          },
          {
            text: 'abcdefgh',
            byteLength: 8,
            start: 1000,
            end: 1008,
            hasNewline: false
          }
        )
      ).toEqual(
        [
          {
            text: 'abcdefgh',
            byteLength: 8,
            start: 1000,
            end: 1008,
            hasNewline: false
          }
        ]
      );
    });

    it('should replace part of existing marker if the new is shorter', () => {
      expect(
        combineSingleLine(
          {
            isMissingMarker: true,
            byteLength: 4,
            start: 1000,
            end: 1004,
            hasNewline: false
          },
          {
            text: 'zq',
            byteLength: 2,
            start: 1000,
            end: 1002,
            hasNewline: false
          }
        )
      ).toEqual(
        [
          {
            text: 'zq',
            byteLength: 2,
            start: 1000,
            end: 1002,
            hasNewline: false
          },
          {
            isMissingMarker: true,
            byteLength: 2,
            start: 1002,
            end: 1004,
            hasNewline: false
          }
        ]
      );
    });
  });

  describe('with new text not at beginning', () => {
    it('should partially replace existing if the new goes beyond it', () => {
      expect(
        combineSingleLine(
          {
            text: 'asdf',
            byteLength: 4,
            start: 1000,
            end: 1004,
            hasNewline: false
          },
          {
            text: '12',
            byteLength: 2,
            start: 1002,
            end: 1004,
            hasNewline: false
          }
        )
      ).toEqual(
        [
          {
            text: 'as12',
            byteLength: 4,
            start: 1000,
            end: 1004,
            hasNewline: false
          }
        ]
      );

      expect(
        combineSingleLine(
          {
            text: 'asdf',
            byteLength: 4,
            start: 1000,
            end: 1004,
            hasNewline: false
          },
          {
            text: '12345',
            byteLength: 5,
            start: 1002,
            end: 1007,
            hasNewline: false
          }
        )
      ).toEqual(
        [
          {
            text: 'as12345',
            byteLength: 7,
            start: 1000,
            end: 1007,
            hasNewline: false
          }
        ]
      );
    });

    it('should splice new into middle of existing if the new is shorter', () => {
      expect(
        combineSingleLine(
          {
            text: 'abcdefgh',
            byteLength: 8,
            start: 1000,
            end: 1008,
            hasNewline: true
          },
          {
            text: '12',
            byteLength: 2,
            start: 1003,
            end: 1005,
            hasNewline: false
          }
        )
      ).toEqual(
        [
          {
            text: 'abc12fgh',
            byteLength: 8,
            start: 1000,
            end: 1008,
            hasNewline: true
          }
        ]
      );
    });

    it('should splice into existing marker if the new goes beyond it', () => {
      expect(
        combineSingleLine(
          {
            isMissingMarker: true,
            byteLength: 8,
            start: 1000,
            end: 1008,
            hasNewline: false
          },
          {
            text: '12',
            byteLength: 2,
            start: 1002,
            end: 1004,
            hasNewline: false
          }
        )
      ).toEqual(
        [
          {
            isMissingMarker: true,
            byteLength: 2,
            start: 1000,
            end: 1002,
            hasNewline: false
          },
          {
            text: '12',
            byteLength: 2,
            start: 1002,
            end: 1004,
            hasNewline: false
          },
          {
            isMissingMarker: true,
            byteLength: 4,
            start: 1004,
            end: 1008,
            hasNewline: false
          }
        ]
      );
    });

    it('should replace part of existing marker if the new is shorter', () => {
      expect(
        combineSingleLine(
          {
            isMissingMarker: true,
            byteLength: 8,
            start: 1000,
            end: 1008,
            hasNewline: false
          },
          {
            text: '1234567890',
            byteLength: 10,
            start: 1002,
            end: 1012,
            hasNewline: false
          }
        )
      ).toEqual(
        [
          {
            isMissingMarker: true,
            byteLength: 2,
            start: 1000,
            end: 1002,
            hasNewline: false
          },
          {
            text: '1234567890',
            byteLength: 10,
            start: 1002,
            end: 1012,
            hasNewline: false
          }
        ]
      );
    });
  });
});
