import fetch from 'isomorphic-fetch';

export const FETCH_STATUS = 'FETCH_STATUS';
export const FETCH_STATUS_STARTED = 'FETCH_STATUS_STARTED';
export const FETCH_STATUS_ERROR = 'FETCH_STATUS_ERROR';
export const FETCH_STATUS_SUCCESS = 'FETCH_STATUS_SUCCESS';

export function fetchStatus() {
  return function (dispatch) {
    dispatch(fetchStatusStarted());

    return fetch(`${config.apiRoot}/state`, {
      credentials: 'include'
    })
      .then(response => response.json())
      .then(json => {
        dispatch(fetchStatusSuccess(json));
      })
      .catch(ex => {
        dispatch(fetchStatusError(ex));
      });
  };
}

export function fetchStatusStarted() {
  return { type: FETCH_STATUS_STARTED };
}

export function fetchStatusError(error) {
  return { type: FETCH_STATUS_ERROR, error: error };
}

export function fetchStatusSuccess(data) {
  return { type: FETCH_STATUS_SUCCESS, data: data };
}
