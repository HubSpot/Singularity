import { TextEncoder } from 'text-encoding'; // polyfill
import fetch from 'isomorphic-fetch';

const frameworkName = 'SINGULARITY_TAILER';

/* GENERIC CHUNK ACTIONS */
const TE = new TextEncoder();

export const ADD_FILE_CHUNK = `${frameworkName}_ADD_FILE_CHUNK`;
export const addFileChunk = (id, chunk) => ({
  type: ADD_FILE_CHUNK,
  id,
  chunk
});

export const SET_FILE_SIZE = `${frameworkName}_SET_FILE_SIZE`;
export const setFileSize = (id, fileSize) => ({
  type: SET_FILE_SIZE,
  id,
  fileSize
});

/* GENERAL ACTIONS */
export const TOGGLE_ANSI_COLORING = `${frameworkName}_TOGGLE_ANSI_COLORING`;
export const toggleAnsiColoring = () => ({
  type: TOGGLE_ANSI_COLORING
});

/* GENERAL API HELPERS */
const checkStatus = (response) => {
  if (response.status >= 200 && response.status < 300) {
    return response;
  }
  const error = new Error(response.statusText);
  error.response = response;
  throw error;
};

const parseJSON = (response) => {
  return response.json();
};

/* SINGULARITY SANDBOX API */

// must be used before calling a fetch
// this sets the Singularity API root
export const SANDBOX_SET_API_ROOT = `${frameworkName}_SANDBOX_SET_API_ROOT`;
export const sandboxSetApiRoot = (apiRoot) => ({
  type: SANDBOX_SET_API_ROOT,
  apiRoot
});


const SANDBOX_FETCH_CHUNK = `${frameworkName}_SANDBOX_FETCH_CHUNK`;

export const SANDBOX_FETCH_CHUNK_STARTED = `${SANDBOX_FETCH_CHUNK}_STARTED`;
export const SANDBOX_FETCH_CHUNK_ERROR = `${SANDBOX_FETCH_CHUNK}_ERROR`;

export const sandboxFetchChunk = (id, taskId, path, start, end, config) => {
  return (dispatch) => {
    dispatch({
      type: SANDBOX_FETCH_CHUNK_STARTED,
      startedAt: Date.now(),
      id,
      start,
      end
    });
    const apiRoot = config.singularityApiRoot;
    const query = `?path=${path}&offset=${start}&length=${end - start}`;
    const apiPath = `${apiRoot}/sandbox/${taskId}/read${query}`;

    return fetch(apiPath, {credentials: 'include'})
      .then(checkStatus)
      .then(parseJSON)
      .then(({data, offset}) => {
        // the API lies, so let's just figure out the bytelength ourselves
        // this code can't take lies.
        const encodedData = TE.encode(data);
        const byteLength = encodedData.byteLength;
        return dispatch(addFileChunk(id, {
          text: data,
          start: offset,
          end: offset + byteLength,
          byteLength
        }));
      }).catch((error) => {
        return dispatch({
          type: SANDBOX_FETCH_CHUNK_ERROR,
          name: error.name,
          message: error.message
        });
      });
  };
};

const SANDBOX_FETCH_LENGTH = `${frameworkName}_SANDBOX_FETCH_LENGTH`;

export const SANDBOX_FETCH_LENGTH_STARTED = `${SANDBOX_FETCH_LENGTH}_STARTED`;
export const SANDBOX_FETCH_LENGTH_ERROR = `${SANDBOX_FETCH_LENGTH}_ERROR`;

export const sandboxGetLength = (id, taskId, path, config) => {
  return (dispatch) => {
    dispatch({
      type: SANDBOX_FETCH_LENGTH_STARTED,
      startedAt: Date.now(),
      id
    });

    const apiRoot = config.singularityApiRoot;
    const query = `?path=${path}&length=${0}`;
    const apiPath = `${apiRoot}/sandbox/${taskId}/read${query}`;

    return fetch(apiPath, {credentials: 'include'})
      .then(checkStatus)
      .then(parseJSON)
      .then(({offset}) => {
        return dispatch(setFileSize(id, offset));
      }).catch((error) => {
        return dispatch({
          type: SANDBOX_FETCH_LENGTH_ERROR,
          name: error.name,
          message: error.message
        });
      });
  };
};
