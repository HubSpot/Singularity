import { TextEncoder } from 'text-encoding'; // polyfill
import fetch from 'isomorphic-fetch';

const frameworkName = 'SINGULARITY_TAILER';

/* GENERIC CHUNK ACTIONS */
const TE = new TextEncoder();

export const ADD_FILE_CHUNK = `${frameworkName}_ADD_FILE_CHUNK`;
// the returned chunk may have a different start and end point than we requested
// but we still want to know what we originally requested so we can mark that
// request done
export const addFileChunk = (id, chunk, requestedStart, requestedEnd) => ({
  type: ADD_FILE_CHUNK,
  id,
  chunk,
  requestedStart,
  requestedEnd
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

// API independent chunk fetching actions
const FETCH_CHUNK = `${frameworkName}_FETCH_CHUNK`;

export const FETCH_CHUNK_STARTED = `${FETCH_CHUNK}_STARTED`;
const fetchChunkStarted = (apiName, id, start, end) => ({
  type: FETCH_CHUNK_STARTED,
  apiName,
  startedAt: Date.now(),
  id,
  start,
  end
});

export const FETCH_CHUNK_ERROR = `${FETCH_CHUNK}_ERROR`;
const fetchChunkError = (apiName, id, start, end, error) => ({
  type: FETCH_CHUNK_ERROR,
  apiName,
  id,
  start,
  end,
  name: error.name,
  message: error.message
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

const parseText = (response) => {
  return response.text();
};

/* SINGULARITY SANDBOX API */

// must be used before calling a fetch
// this sets the Singularity API root
export const SANDBOX_SET_API_ROOT = `${frameworkName}_SANDBOX_SET_API_ROOT`;
export const sandboxSetApiRoot = (apiRoot) => ({
  type: SANDBOX_SET_API_ROOT,
  apiRoot
});

export const sandboxFetchChunk = (id, taskId, path, start, end, config) => {
  return (dispatch) => {
    dispatch(
      fetchChunkStarted('SANDBOX', id, start, end)
    );

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
        }, start, end));
      }).catch((error) => {
        return dispatch(
          fetchChunkError('SANDBOX', id, start, end, error)
        );
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

/* STANDARD HTTP API */
export const httpFetchChunk = (id, path, start, end) => {
  return (dispatch) => {
    dispatch(
      fetchChunkStarted('HTTP', id, start, end)
    );

    const httpHeaders = new Headers();
    if (start) {
      httpHeaders.append('Range', `bytes=${start}-${end || ''}`);
    }

    const fetchInit = {
      method: 'GET',
      headers: httpHeaders
    };

    return fetch(path, fetchInit)
      .then(checkStatus)
      .then(parseText)
      .then((data) => {
        // the API lies, so let's just figure out the bytelength ourselves
        // this code can't take lies.
        const encodedData = TE.encode(data);
        const byteLength = encodedData.byteLength;
        return dispatch(addFileChunk(id, {
          text: data,
          start,
          end: start + byteLength,
          byteLength
        }, start, end));
      }).catch((error) => {
        return dispatch(
          fetchChunkError('HTTP', id, start, end, error)
        );
      });
  };
};
