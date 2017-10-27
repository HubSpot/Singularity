import { TextEncoder } from 'text-encoding'; // polyfill
import fetch from 'isomorphic-fetch';

const frameworkName = 'SINGULARITY_TAILER';

export const SINGULARITY_TAILER_AJAX_ERROR_EVENT = 'SingularityTailerAjaxError';

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

export const UNLOAD_FILE = `${frameworkName}_UNLOAD_FILE`;
export const unloadFile = (id) => ({
  type: UNLOAD_FILE,
  id
});

export const STOP_TAILING = `${frameworkName}_STOP_TAILING`;
export const stopTailing = (id) => ({
  type: STOP_TAILING,
  id
});

export const START_TAILING = `${frameworkName}_START_TAILING`;
export const startTailing = (id) => ({
  type: START_TAILING,
  id
});

export const UNLOAD_FILE_CHUNK = `${frameworkName}_UNLOAD_FILE_CHUNK`;
export const unloadFileChunk = (id, index) => ({
  type: UNLOAD_FILE_CHUNK,
  id,
  index
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

export const TOGGLE_FETCH_OVERSCAN = `${frameworkName}_TOGGLE_FETCH_OVERSCAN`;
export const toggleFetchOverscan = () => ({
  type: TOGGLE_FETCH_OVERSCAN
});

// Infinite loader actions
export const RENDERED_LINES = `${frameworkName}_RENDERED_LINES`;
export const renderedLines = (id, startIndex, stopIndex, overscanStartIndex, overscanStopIndex) => ({
  type: RENDERED_LINES,
  id,
  startIndex,
  stopIndex,
  overscanStartIndex,
  overscanStopIndex
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
const checkStatus = (response, taskId) => {
  if (response.status >= 200 && response.status < 300) {
    return response;
  }
  const error = new Error(response.statusText);
  error.response = response;
  if (document && document.dispatchEvent) {
    document.dispatchEvent(new CustomEvent(SINGULARITY_TAILER_AJAX_ERROR_EVENT, {'detail': {'response': response, 'taskId': taskId}}));
  }
  throw error;
};

const parseJSON = (response) => {
  return response.json();
};

const parseText = (response) => {
  return response.text();
};

/* SINGULARITY SANDBOX API */
export const SANDBOX_MAX_BYTES = 65535;

// must be used before calling a fetch
// this sets the Singularity API root
export const SANDBOX_SET_API_ROOT = `${frameworkName}_SANDBOX_SET_API_ROOT`;
export const sandboxSetApiRoot = (apiRoot) => ({
  type: SANDBOX_SET_API_ROOT,
  apiRoot
});

export const SET_TAIL_INTERVAL_MS = `${frameworkName}_TAIL_INTERVAL_MS`;
export const tailIntervalMs = (tailIntervalMs) => ({
  type: SET_TAIL_INTERVAL_MS,
  tailIntervalMs
});

export const SET_AUTHORIZATION_HEADER = `${frameworkName}_SET_AUTHORIZATION_HEADER`;
export const setAuthorizationHeader = (authorizationHeader) => ({
  type: SET_AUTHORIZATION_HEADER,
  authorizationHeader
});

export const sandboxFetchChunk = (id, taskId, path, start, end, config) => {
  return (dispatch) => {
    dispatch(
      fetchChunkStarted('SANDBOX', id, start, end)
    );

    const apiRoot = config.singularityApiRoot;
    const query = `?path=${path}&offset=${start}&length=${end - start}`;
    const apiPath = `${apiRoot}/sandbox/${taskId}/read${query}`;

    const options = {credentials: 'include'}
    if (config.authorizationHeader) {
      options['headers'] = {'Authorization': config.authorizationHeader}
    }

    return fetch(apiPath, options)
      .then((r) => {return checkStatus(r, taskId)})
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

export const sandboxFetchLength = (id, taskId, path, config) => {
  return (dispatch) => {
    dispatch({
      type: SANDBOX_FETCH_LENGTH_STARTED,
      startedAt: Date.now(),
      id
    });

    const apiRoot = config.singularityApiRoot;
    const query = `?path=${path}&length=${0}`;
    const apiPath = `${apiRoot}/sandbox/${taskId}/read${query}`;

    const options = {credentials: 'include'}
    if (config.authorizationHeader) {
      options['headers'] = {'Authorization': config.authorizationHeader}
    }

    return fetch(apiPath, options)
      .then((r) => {return checkStatus(r, taskId)})
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

export const SANDBOX_FETCH_TAIL = `${frameworkName}_SANDBOX_FETCH_TAIL`;
export const sandboxFetchTail = (id, taskId, path, config) => {
  return (dispatch) => {
    dispatch(sandboxFetchLength(id, taskId, path, config)).then(
      (lengthAction) => {
        const start = Math.max(lengthAction.fileSize - SANDBOX_MAX_BYTES, 0);
        const end = start + SANDBOX_MAX_BYTES;

        return dispatch(sandboxFetchChunk(
          id,
          taskId,
          path,
          start,
          end,
          config
        ));
      }
    );
  };
};

/* BLAZAR LOG API */
export const BLAZAR_LOG_MAX_BYTES = 65535;

// must be used before calling a fetch
// this sets the Blazar API root
export const BLAZAR_SET_API_ROOT = `${frameworkName}_BLAZAR_SET_API_ROOT`;
export const blazarSetApiRoot = (apiRoot) => ({
  type: BLAZAR_SET_API_ROOT,
  apiRoot
});

export const blazarLogFetchChunk = (id, buildId, start, end, config) => {
  return (dispatch) => {
    if (start === end) {
      console.log('skipping', start, end)
      return Promise.resolve();
    }

    dispatch(
      fetchChunkStarted('BLAZAR_LOG', id, start, end)
    );

    const apiRoot = config.blazarApiRoot;
    const query = `?offset=${start}&length=${end - start}`;
    const apiPath = `${apiRoot}/modules/builds/${buildId}/log${query}`;

    const options = {credentials: 'include'}
    if (config.authorizationHeader) {
      options['headers'] = {'Authorization': config.authorizationHeader}
    }

    return fetch(apiPath, options)
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
          fetchChunkError('BLAZAR_LOG', id, start, end, error)
        );
      });
  };
};

const BLAZAR_LOG_FETCH_LENGTH = `${frameworkName}_BLAZAR_LOG_FETCH_LENGTH`;

export const BLAZAR_LOG_FETCH_LENGTH_STARTED = `${BLAZAR_LOG_FETCH_LENGTH}_STARTED`;
export const BLAZAR_LOG_FETCH_LENGTH_ERROR = `${BLAZAR_LOG_FETCH_LENGTH}_ERROR`;

export const blazarLogFetchLength = (id, buildId, config) => {
  return (dispatch) => {
    dispatch({
      type: BLAZAR_LOG_FETCH_LENGTH_STARTED,
      startedAt: Date.now(),
      id
    });

    const apiRoot = config.blazarApiRoot;
    const apiPath = `${apiRoot}/modules/builds/${buildId}/log/size`;

    const options = {credentials: 'include'}
    if (config.authorizationHeader) {
      options['headers'] = {'Authorization': config.authorizationHeader}
    }

    return fetch(apiPath, options)
      .then(checkStatus)
      .then(parseJSON)
      .then(({size}) => {
        return dispatch(setFileSize(id, size));
      }).catch((error) => {
        return dispatch({
          type: BLAZAR_LOG_FETCH_LENGTH_ERROR,
          name: error.name,
          message: error.message
        });
      });
  };
};

export const BLAZAR_LOG_FETCH_TAIL = `${frameworkName}_BLAZAR_LOG_FETCH_TAIL`;
export const blazarLogFetchTail = (id, buildId, config) => {
  return (dispatch) => {
    dispatch(blazarLogFetchLength(id, buildId, config)).then(
      (lengthAction) => {
        const start = Math.max(lengthAction.fileSize - BLAZAR_LOG_MAX_BYTES, 0);
        const end = start + BLAZAR_LOG_MAX_BYTES;

        return dispatch(blazarLogFetchChunk(
          id,
          buildId,
          start,
          end,
          config
        ));
      }
    );
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
