import fetch from 'isomorphic-fetch';

export const FETCH_SLAVES = 'FETCH_SLAVES';
export const FETCH_SLAVES_STARTED = 'FETCH_SLAVES_STARTED';
export const FETCH_SLAVES_ERROR = 'FETCH_SLAVES_ERROR';
export const FETCH_SLAVES_SUCCESS = 'FETCH_SLAVES_SUCCESS';

export function fetchSlaves() {
  return function (dispatch) {
    dispatch(fetchSlavesStarted());

    return fetch(`${ config.apiRoot }/slaves/`, {
      credentials: 'include'
    })
      .then(response => response.json())
      .then(json => {
        dispatch(fetchSlavesSuccess(json));
      })
      .catch(ex => {
        dispatch(fetchSlavesError(ex));
      });
  };
}

export function fetchSlavesStarted() {
  return { type: FETCH_SLAVES_STARTED };
}

export function fetchSlavesError(error) {
  return { type: FETCH_SLAVES_ERROR, error: error };
}

export function fetchSlavesSuccess(data) {
  return { type: FETCH_SLAVES_SUCCESS, data: data };
}