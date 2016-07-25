const frameworkName = 'SINGULARITY_TAILER';

export const ADD_CHUNK = `${frameworkName}_ADD_CHUNK`;

export const addChunk = (id, chunk) => ({
  type: ADD_CHUNK,
  id,
  chunk
});
