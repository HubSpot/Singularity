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
        {
          text: 'asdf',
          byteLength: 4,
          start: 1000,
          end: 1005,
          hasNewline: true
        }
      );
    });
  });
});
