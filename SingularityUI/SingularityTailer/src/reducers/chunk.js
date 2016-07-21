import { TextEncoder, TextDecoder } from 'text-encoding'; // polyfill

import { ADD_CHUNK } from '../actions';

const TE = new TextEncoder();
const TD = new TextDecoder();

// see big comment at bottom of file for perf info
export const splitChunkIntoLines = (chunk) => {
  const { data, offset, length } = chunk;

  const lines = data.split('\n');
  const byteLengths = lines.map((line) => TE.encode(line).byteLength);

  const partialLines = [];

  let currentOffset = offset;
  lines.forEach((line, i) => {
    const hasNewline = i !== lines.length - 1;
    // add newline byte
    const lineLength = byteLengths[i] + (hasNewline ? 1 : 0);
    partialLines[i] = {
      text: line,
      byteLength: byteLengths[i],
      start: currentOffset,
      end: currentOffset += lineLength,
      hasNewline
    };
  });

  return partialLines;
};

const initialState = {

};

const createMissingMarker = (start, end = undefined) => ({
  isMissingMarker: true,
  byteLength: (end !== undefined) ? (end - start) : undefined,
  start,
  end,
  hasNewline: false
});

// get the first and offsets of a list of Partial Lines (sorted)
// partialLines must have length
const getBookends = (partialLines) => {
  if (!partialLines.length) {
    console.error( // eslint-disable-line no-console
      'Assertion failed: partialLines.length',
      partialLines
    );
  }
  // the first line and last line could be the same
  const firstLine = partialLines[0];
  const lastLine = partialLines[partialLines.length - 1];

  return {
    firstOffset: firstLine.start,
    lastOffset: lastLine.end
  };
};

export const combineSingleLine = (existing, incoming) => {
  const incomingStart = incoming.start;
  const incomingEnd = incoming.start + incoming.byteLength;
  const existingStart = existing.start;
  const existingEnd = existing.start + existing.byteLength;
  // condition: new text is at beginning
  if (existingStart === incomingStart) {
    if (existingEnd && existingEnd > incomingEnd) {
      // existing line goes beyond what we have here
      const existingBytes = TE.encode(existing.text);
      const newBytes = TE.encode(incoming.text);

      const last = existingBytes.subarray(
        existingEnd - incomingEnd,
        existingEnd - incomingStart
      );

      // If this can be made better, it should be!
      // allocate a new array for both, and decode the text
      const combinedByteLength = newBytes.byteLength + last.byteLength;
      const combined = new Uint8Array(combinedByteLength);
      combined.set(newBytes);
      combined.set(last, newBytes.byteLength);

      return {
        text: TD.decode(combined),
        byteLength: combinedByteLength,
        start: incomingStart,
        end: existing.end, // has newline if there is one
        hasNewline: existing.hasNewline
      };
    }
    // existing line is completely encompassed by this line
    return incoming;
  }
  // condition: new text is not at the beginning


  // condition: new text is at end
  // condition: new text is only in middle
  // condition: new text encompasses entire existing line

  if (existing.hasOwnProperty('text')) {
    // text being replaced by text

  }
};

/*
Lines can be (and usually are) incomplete, let's place the new lines in the
list of partial lines, combining lines where necessary.
*/
export const combinePartialLines = (existingPartialLines, newPartialLines) => {
  if (!newPartialLines.length) {
    return existingPartialLines;
  }

  const { firstOffset, lastOffset } = getBookends(partialLines);

  // looks like we already have some of this file, let's merge these new lines in

  // search for the line/marker that contains our first offset
  const intersectIndex = existingPartialLines.findIndex((pl) => {
    return pl.start >= firstOffset;
  });

  if (intersectIndex === -1) {
    // I can't think of how this would happen, but it probably can
    console.error( // eslint-disable-line no-console
      'Assertion failed: intersectIndex !== -1',
      existingPartialLines,
      newPartialLines
    );

    // bail
    return existingPartialLines;
  }

  // Okay, we have an intersection point
  const intersection = existingPartialLines[intersectIndex];
  if (intersection.hasOwnProperty('text')) {
    // this is a loaded piece, handle carefully \u{1F52B}

    // we want to replace the part that we have with the part
    // TODO: some of this

    // repeat intersection finding until we've exited this
  }
  // okay, this is a marker we're looking at, let's figure out if we need to
  // shrink it, remove it, or shrink it and put a new one at the end.
};

const initializeLog = (partialLines) => {
  if (!partialLines.length) {
    // we don't have any data, but we should init the log anyway
    // create a tail marker (this is similar to the case where we have data
    // starting from the beginning, but this is *only* the tail marker
    return [
      createMissingMarker(0)
    ];
  }

  // we have data
  const { firstOffset, lastOffset } = getBookends(partialLines);

  const missingTailMarker = createMissingMarker(lastOffset);
  if (firstOffset !== 0) {
    // create a missing marker for the chunk before this
    const missingDataMarker = createMissingMarker(0, firstOffset);

    return [
      missingDataMarker,
      ...partialLines,
      missingTailMarker
    ];
  }

  // looks like we're starting this file from the beginning
  return [
    ...partialLines,
    missingTailMarker
  ];
};

const addChunkReducer = (state, action) => {
  const { id, chunk } = action;
  const partialLines = splitChunkIntoLines(chunk);

  // condition: has not been init
  if (!state.hasOwnProperty(id)) {
    return {
      [id]: initializeLog(partialLines),
      ...state
    };
  }

  // has been init but has no new data
  if (!chunk.length) {
    return state;
  }

  // has been init and has new data
  return {
    [id]: combinePartialLines(state[id], partialLines),
    ...state
  };
};

const chunkReducer = (state = initialState, action) => {
  switch (action.type) {
    case ADD_CHUNK:
      return addChunkReducer(state, action);
    default:
      return state;
  }
};

export default chunkReducer;

/*
Justifying the algorithm used:

hipster text actual byte length: 2399 (generated at: http://hipsum.co/?paras=4&type=hipster-centric)

Speed tests:

benchmark = (f) => { then = Date.now(); for(let i=0; i<100000; i++) { f() } console.log(Date.now() - then); }
const te = new TextEncoder();

Byte count
----------

TextEncoder:
benchmark(() => { te.encode(hipster).byteLength; });
-> 1216

byteCount:
function byteCount(s) {
  return encodeURI(s).split(/%..|./).length - 1;
}
benchmark(() => { byteCount(hipster); });
-> 19114

For small strings, byteCount is way faster, because it doesn't need to make a Uint8Array
For big strings, TextEncoder is way faster, because it's not a hack.

Split
-----

Uint8Array:
benchmark(() => {
  byteArray.reduce((newLines, b, index) => ((b === 10) ? newLines.concat([index]) : newLines), [])
});
-> 13624

let newLines;
benchmark(() => {
  newLines = []; byteArray.forEach((b, index) => { (b === 10) && newLines.push(index); });
});
-> 23690

String split:
benchmark(() => { hipster.split('\n') });
-> 19


Combination
-----------
String split then byte count with byteCount:
benchmark(() => hipster.split("p").map((l) => byteCount(l)))
-> 23771

benchmark(() => hipster.split("p").map((l) => te.encode(l).byteLength))
-> 8267
*/
