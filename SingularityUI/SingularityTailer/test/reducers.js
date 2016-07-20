/* eslint-env node, mocha */
import expect from 'expect';
import reducer from '../src/reducers';
import * as types from '../src/actions';

const initialState = {

};

describe('log initalize reducer', () => {
  it('should return', () => {
    expect(
      reducer(undefined, {})
    ).toEqual([
    ]);
  });
});
