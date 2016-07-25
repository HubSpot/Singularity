import { TextEncoder, TextDecoder } from 'text-encoding'; // polyfill
import { List } from 'immutable';

import { ADD_CHUNK, SET_LOG_SIZE } from '../actions';

const TE = new TextEncoder();
const TD = new TextDecoder('utf-8', {fatal: true});

// Key concept: rangeLike
// most of the objects in this file use the concept of a range-like, which
// is a duck-typed object that has a `start` and `end` field.
// the start and end fields are byte offsets into the file that we are dealing
// with. Many functions use them to figure out where to splice and dice lists.

export const createMissingMarker = (start, end) => ({
  isMissingMarker: true,
  byteLength: end - start,
  start,
  end,
  hasNewline: false
});

export const splitChunkIntoLines = (chunk) => {
  const { text, start } = chunk; // { end, byteLength } should also be provided

  const lines = new List(text.split('\n'));
  const byteLengths = lines.map((line) => TE.encode(line).byteLength);

  let partialLines = new List();

  let currentOffset = start;
  lines.forEach((line, i) => {
    const hasNewline = i !== lines.size - 1;
    // add newline byte
    const lineLength = byteLengths.get(i) + (hasNewline ? 1 : 0);
    partialLines = partialLines.insert(i, {
      text: line,
      byteLength: byteLengths.get(i),
      start: currentOffset,
      end: currentOffset += lineLength,
      hasNewline
    });
  });

  return partialLines;
};
/*
Justifying the byteLength algorithm used:

hipster text actual byte length: 2399
(generated at: http://hipsum.co/?paras=4&type=hipster-centric)

Speed tests:

benchmark = (f) => {
  then = Date.now();
  for(let i=0; i<100000; i++) { f() }
  console.log(Date.now() - then);
}
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

For small strings, byteCount is way faster,
because it doesn't need to make a Uint8Array

For big strings, TextEncoder is way faster,
because it's not a hack.

Split
-----

Uint8Array:
benchmark(() => {
  byteArray.reduce((newLines, b, index) => (
    (b === 10) ? newLines.concat([index]) : newLines), []
  )
});
-> 13624

let newLines;
benchmark(() => {
  newLines = []; byteArray.forEach((b, index) => {
    (b === 10) && newLines.push(index);
  });
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

// get the byte start and end of a list
const getBookends = (list) => {
  if (!list.size) {
    return {
      start: 0,
      end: 0
    };
  }

  return {
    start: list.first().start,
    end: list.last().end
  };
};

// Checks if two chunks/ranges overlap
const isOverlapping = (c1, c2, inclusive = false) => {
  const maxStart = Math.max(c1.start, c2.start);
  const minEnd = Math.min(c1.end, c2.end);
  if (inclusive) {
    return maxStart <= minEnd;
  }
  return maxStart < minEnd;
};

// rangeLike can be a range object (start, end), a chunk, or a line
// (they all have start and end byte fields)
const findOverlap = (list, rangeLike, inclusive = false) => {
  return {
    startIndex: list.findIndex(
      (c) => isOverlapping(rangeLike, c, inclusive)
    ),
    endIndex: list.findLastIndex(
      (c) => isOverlapping(rangeLike, c, inclusive)
    )
  };
};

const getIndexRange = (list, indexRange) => {
  const { startIndex, endIndex } = indexRange;
  if (startIndex === -1) {
    return new List();
  }

  return list.slice(startIndex, endIndex + 1);
};

const getOverlap = (list, rangeLike, inclusive = false) => {
  return getIndexRange(list, findOverlap(list, rangeLike, inclusive));
};

// incoming: single chunk
export const mergeChunks = (existing, incoming) => {
  const replacementRange = findOverlap(existing, incoming);
  const intersectingChunks = getIndexRange(existing, replacementRange);

  if (intersectingChunks.size) {
    // okay, we know that there are some chunks that overlap with us
    // we only need to merge the first and last and only if each goes beyond
    // the chunks
    let firstBytes;
    const firstIntersectingChunk = intersectingChunks.first();
    if (firstIntersectingChunk.start < incoming.start) {
      // let's slice and dice this one
      // this is a loaded piece, handle carefully \u{1F52B}
      firstBytes = TE.encode(firstIntersectingChunk.text).subarray(
        0,
        incoming.start - firstIntersectingChunk.start
      );
    }

    // the last could also be the first, but that's fine
    let lastBytes;
    const lastIntersectingChunk = intersectingChunks.last();
    if (lastIntersectingChunk.end > incoming.end) {
      // let's also slice and dice this one
      // this math can almost certainly be simplified, but this works
      lastBytes = TE.encode(lastIntersectingChunk.text).subarray(
        lastIntersectingChunk.byteLength -
        (lastIntersectingChunk.end - incoming.end)
      );
    }

    const chunksToReplace = (
      replacementRange.endIndex - replacementRange.startIndex + 1
    );

    let newChunk;
    // combine the bytes together if needed
    if (firstBytes || lastBytes) {
      // we have to convert the incoming chunk to bytes to combine
      const incomingBytes = TE.encode(incoming.text);

      // If this can be made better, it should be!
      // allocate a new array for both, and decode the text
      const combinedByteLength = (
        (firstBytes ? firstBytes.byteLength : 0) +
        incomingBytes.byteLength +
        (lastBytes ? lastBytes.byteLength : 0)
      );

      const combined = new Uint8Array(combinedByteLength);
      if (firstBytes) {
        combined.set(firstBytes);
        combined.set(incomingBytes, firstBytes.byteLength);
      } else {
        combined.set(incomingBytes);
      }

      if (lastBytes) {
        // oh you think you're clever don't you
        combined.set(lastBytes, combinedByteLength - lastBytes.byteLength);
      }

      newChunk = {
        text: TD.decode(combined),
        byteLength: combinedByteLength,
        start: (firstBytes ? firstIntersectingChunk.start : incoming.start),
        end: (lastBytes ? lastIntersectingChunk.end : incoming.end)
      };
    } else {
      newChunk = incoming;
    }

    return existing.splice(
      replacementRange.startIndex,
      chunksToReplace,
      newChunk
    );
  }
  // oh, this is so much more simple
  // find where to put this chunk
  const indexBefore = existing.findIndex((c) => incoming.start >= c.end);

  // works even if indexBefore === -1
  return existing.insert(indexBefore + 1, incoming);
};

export const createLines = (chunks, range) => {
  // get chunks that overlap a byte range (inclusive)
  return getOverlap(chunks, range, true).reduce(
    (accumulatedLines, c) => {
      const chunkLines = getOverlap(splitChunkIntoLines(c), range, true);
      if (accumulatedLines.size && chunkLines.size) {
        const existingPart = accumulatedLines.last();
        const newPart = chunkLines.first();
        // create missing marker if the parts don't line up
        if (existingPart.end !== newPart.start) {
          accumulatedLines = accumulatedLines.push(createMissingMarker(
            existingPart.end,
            newPart.start
          ));
        } else if (!existingPart.hasNewline) {
          // combine partial lines
          accumulatedLines = accumulatedLines.set(-1, {
            text: existingPart.text + newPart.text,
            byteLength: existingPart.byteLength + newPart.byteLength,
            start: existingPart.start,
            end: newPart.end,
            hasNewline: newPart.hasNewline
          });

          return accumulatedLines.concat(chunkLines.rest());
        }
      }
      return accumulatedLines.concat(chunkLines);
    },
    new List()
  );
};

// incoming: List of lines
export const mergeLines = (existing, incoming, replacementRange) => {
  const generatedByteRange = getBookends(incoming);
  const replacementByteRange = {
    start: existing.get(replacementRange.startIndex).start,
    end: existing.get(replacementRange.endIndex).end
  };

  // see if we need to add a missing marker to the start
  if (generatedByteRange.start > replacementByteRange.start) {
    incoming = incoming.unshift(createMissingMarker(
      replacementByteRange.start,
      generatedByteRange.start
    ));
  }

  // and to the end
  if (generatedByteRange.end < replacementByteRange.end) {
    incoming = incoming.push(createMissingMarker(
      generatedByteRange.end,
      replacementByteRange.end
    ));
  }

  return existing
    .slice(0, replacementRange.startIndex)
    .concat(incoming)
    .concat(existing.slice(replacementRange.endIndex + 1));
};

export const addChunkReducer = (state, action) => {
  const { id, chunk } = action;

  if (!state[id]) {
    const chunks = mergeChunks(new List(), chunk);
    const bookends = getBookends(chunks);
    let lines = createLines(chunks, bookends);

    if (bookends.start !== 0) {
      lines = lines.unshift(
        createMissingMarker(0, bookends.start)
      );
    }

    return {
      ...state,
      [id]: {
        chunks,
        lines,
        logSize: bookends.end
      }
    };
  }

  // has been init but has no new data
  if (!chunk.byteLength) {
    return state;
  }

  // has been init and has new data
  const chunks = mergeChunks(state[id].chunks, chunk);
  const replacementRange = findOverlap(state[id].lines, chunk, true);
  return {
    ...state,
    [id]: {
      chunks,
      lines: mergeLines(
        state[id].lines,
        createLines(chunks, chunk),
        replacementRange
      ),
      logSize: Math.max(state[id].logSize, chunk.end)
    }
  };
};

export const removeLogReducer = (state, action) => {
  const { id } = action;

  if (state[id]) {
    return {
      ...state,
      [id]: undefined
    };
  }

  return state;
};

const initialState = {};

const chunkReducer = (state = initialState, action) => {
  switch (action.type) {
    case ADD_CHUNK:
      try {
        return addChunkReducer(state, action);
      } catch (e) {
        console.warn( // eslint-disable-line no-console
          `LogTailer caught ${e.name}. Invalidating log`,
          e
        );
        return addChunkReducer(
          removeLogReducer(state, action),
          action
        );
      }
    case SET_LOG_SIZE:
      return {
        ...state,
        [action.id]: {
          chunks: state[action.id].chunks,
          lines: state[action.id].lines,
          logSize: Math.max(state[action.id].logSize, action.logSize)
        }
      };
    default:
      return state;
  }
};

export default chunkReducer;
