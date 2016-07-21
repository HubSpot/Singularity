/* eslint-env node, mocha */
import expect from 'expect';
import reducer from '../src/reducers';

import * as types from '../src/actions';


import { splitChunkIntoLines } from '../src/reducers/chunk';

describe('chunk splitter', () => {
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
        byteLength: 5,
        start: 0,
        end: 5,
        hasNewline: true
      },
      {
        text: 'asdg',
        byteLength: 5,
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
        byteLength: 5,
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
});
