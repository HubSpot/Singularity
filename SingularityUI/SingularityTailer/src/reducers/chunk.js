import { TextEncoder, TextDecoder } from 'text-encoding'; // polyfill
import { List } from 'immutable';

import { ADD_CHUNK } from '../actions';

const TE = new TextEncoder();
const TD = new TextDecoder('utf-8', {fatal: true});

export const createMissingMarker = (start, end = undefined) => ({
  isMissingMarker: true,
  byteLength: (end !== undefined) ? (end - start) : undefined,
  start,
  end,
  hasNewline: false
});

// see big comment at bottom of file for perf info
export const splitChunkIntoLines = (chunk) => {
  const { text, start, end, byteLength } = chunk;

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

// get the first and offsets of a list of Partial Lines (sorted)
// partialLines must have length
const getBookends = (partialLines) => {
  if (!partialLines.length) {
    console.error( // eslint-disable-line no-console
      'LogTailer assertion failed: partialLines.length',
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

// Checks if two chunks overlap
const isOverlapping = (c1, c2) => {
  return Math.max(c1.start, c2.start) < Math.min(c1.end, c2.end);
};

// rangeLike can be a range object (start, end), a chunk, or a line
// (they all have start and end byte fields)
const findOverlap = (chunks, rangeLike) => {
  return {
    startIndex: chunks.findIndex((c) => isOverlapping(rangeLike, c)),
    endIndex: chunks.findLastIndex((c) => isOverlapping(rangeLike, c))
  };
};

const getIndexRange = (list, indexRange) => {
  const { startIndex, endIndex } = indexRange;
  if (startIndex === -1) {
    return new List();
  }

  return list.slice(startIndex, endIndex + 1);
};

const getOverlap = (list, rangeLike) => {
  return getIndexRange(list, findOverlap(list, rangeLike));
};

export const mergeChunks = (incoming, existing) => {
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
  // get chunks that overlap a byte range
  return getOverlap(chunks, range).reduce(
    (accumulatedLines, c) => {
      const chunkLines = getOverlap(splitChunkIntoLines(c), range);
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

export const addChunkReducer = (state, action) => {
  const { id, chunk } = action;
  const partialLines = splitChunkIntoLines(chunk);

  // condition: has not been init
  if (!state[id]) {
    return {
      ...state,
      [id]: initializeLog(partialLines)
    };
  }

  // has been init but has no new data
  if (!chunk.length) {
    return state;
  }

  // has been init and has new data
  return {
    ...state,
    [id]: combinePartialLines(state[id], partialLines)
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

const initialState = {

};

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
