const frameworkName = 'SINGULARITY_TAILER';

export const ADD_CHUNK = `${frameworkName}_ADD_CHUNK`;

export const addChunk = (chunk) => ({
  type: ADD_CHUNK,
  data: chunk
});
