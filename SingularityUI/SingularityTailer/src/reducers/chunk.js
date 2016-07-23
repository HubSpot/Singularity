import { TextEncoder, TextDecoder } from 'text-encoding'; // polyfill
import { List } from 'immutable';

import { ADD_CHUNK } from '../actions';

const TE = new TextEncoder();
const TD = new TextDecoder('utf-8', {fatal: true});

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

const findReplacementRange = (incoming, existing) => {
  return {
    start: existing.findIndex((c) => isOverlapping(incoming, c)),
    end: existing.findLastIndex((c) => isOverlapping(incoming, c))
  };
};

const getChunksInRange = (chunks, range) => {
  const { start, end } = range;
  if (start === -1) {
    return new List();
  }

  return chunks.slice(start, end + 1);
};

export const mergeChunks = (incoming, existing) => {
  const replacementRange = findReplacementRange(incoming, existing);
  const intersectingChunks = getChunksInRange(existing, replacementRange);

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

    const chunksToReplace = replacementRange.end - replacementRange.start + 1;

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
      replacementRange.start,
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


// if the combination fails, an upstream method will catch the DecodingError
// and invalidate the whole log
export const combineSingleLine = (existing, incoming) => {
  const incomingStart = incoming.start;
  const incomingEnd = incoming.start + incoming.byteLength;
  const existingStart = existing.start;
  const existingEnd = existing.start + existing.byteLength;
  // condition: new text is at beginning
  if (existingStart === incomingStart) {
    if (existingEnd && existingEnd > incomingEnd) {
      // existing line goes beyond what we have here
      // new:   [    ]
      // exist: [           ]

      if (existing.isMissingMarker) {
        // marker being partially replaced by text
        return [
          incoming,
          createMissingMarker(
            incoming.end,
            existing.end
          )
        ];
      }

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

      return [
        {
          text: TD.decode(combined), // if this fails, we invalidate the whole log
          byteLength: combinedByteLength,
          start: incomingStart,
          end: existing.end, // has newline if there is one
          hasNewline: existing.hasNewline
        }
      ];
    }
    // existing line is completely encompassed by this line
    // new:   [           ]
    // exist: [        ]
    return [
      incoming
    ];
  }
  // condition: new text is not at the beginning
  if (existingEnd && existingEnd > incomingEnd) {
    // existing line goes beyond this
    // new:     [    ]
    // exist: [           ]
    if (existing.isMissingMarker) {
      return [
        createMissingMarker(
          existingStart,
          incomingStart
        ),
        incoming,
        createMissingMarker(
          incoming.end, // don't account for nl
          existing.end
        )
      ];
    }
    // real text data
    // this is a loaded piece, handle carefully \u{1F52B}
    const existingBytes = TE.encode(existing.text);
    const newBytes = TE.encode(incoming.text);

    const first = existingBytes.subarray(
      0,
      incomingStart - existingEnd
    );

    const last = existingBytes.subarray(
      first.byteLength + newBytes.byteLength
    );

    // If this can be made better, it should be!
    // allocate a new array for both, and decode the text
    const combinedByteLength = first.byteLength + newBytes.byteLength + last.byteLength;
    const combined = new Uint8Array(combinedByteLength);
    combined.set(first);
    combined.set(newBytes, first.byteLength);
    combined.set(last, first.byteLength + newBytes.byteLength);

    return [
      {
        text: TD.decode(combined), // if this fails, we invalidate the whole log
        byteLength: combinedByteLength,
        start: existingStart,
        end: existing.end, // has newline if there is one
        hasNewline: existing.hasNewline
      }
    ];
  }
  // existing line ends before new
  // new:      [    ]
  // exist: [      ]
  if (existing.isMissingMarker) {
    return [
      createMissingMarker(
        existingStart,
        incomingStart
      ),
      incoming
    ];
  }

  const existingBytes = TE.encode(existing.text);
  const newBytes = TE.encode(incoming.text);

  const first = existingBytes.subarray(
    0,
    incomingStart - existingStart
  );

  // If this can be made better, it should be!
  // allocate a new array for both, and decode the text
  const combinedByteLength = first.byteLength + newBytes.byteLength;
  const combined = new Uint8Array(combinedByteLength);
  combined.set(first);
  combined.set(newBytes, first.byteLength);

  return [
    {
      text: TD.decode(combined), // if this fails, we invalidate the whole log
      byteLength: combinedByteLength,
      start: existingStart,
      end: incoming.end, // has newline if there is one
      hasNewline: incoming.hasNewline
    }
  ];
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
      'LogTailer assertion failed: intersectIndex !== -1',
      existingPartialLines,
      newPartialLines
    );

    // bail
    return existingPartialLines;
  }

  // Okay, we have an intersection point
  const intersection = existingPartialLines[intersectIndex];

  // search for the line/marker that contains our first offset
  const lastIntersectIndex = existingPartialLines.findIndex((pl) => {
    return pl.end < lastOffset;
  });

  const mergedLines = [];

  const linesBefore = existingPartialLines.slice(0, intersectIndex);
  if (linesBefore.length) {
    mergedLines.push(...linesBefore);
  }

  const firstMergedLine = combineSingleLine(intersection, newPartialLines[0]);
  if (firstMergedLine.length > 1) {

  }
  // okay let's try this again

  // search for the last intersection point
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
