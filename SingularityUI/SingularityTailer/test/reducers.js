/* eslint-env node, mocha */
import expect from 'expect';
import reducer from '../src/reducers';

import * as types from '../src/actions';


import {
  splitChunkIntoLines,
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
