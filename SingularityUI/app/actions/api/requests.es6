import fetch from 'isomorphic-fetch';

export const FETCH_REQUESTS = 'FETCH_REQUESTS';
export const FETCH_REQUESTS_STARTED = 'FETCH_REQUESTS_STARTED';
export const FETCH_REQUESTS_ERROR = 'FETCH_REQUESTS_ERROR';
export const FETCH_REQUESTS_SUCCESS = 'FETCH_REQUESTS_SUCCESS';

export function fetchRequests() {
  return function (dispatch) {
    dispatch(fetchRequestsStarted());

    return fetch(`${ config.apiRoot }/requests/`, {
      credentials: 'include'
    })
      .then(response => response.json())
      .then(json => {
        dispatch(fetchRequestsSuccess(json));
      })
      .catch(ex => {
        dispatch(fetchRequestsError(ex));
      });
  };
}

export function fetchRequestsStarted() {
  return { type: FETCH_REQUESTS_STARTED };
}

export function fetchRequestsError(error) {
  return { type: FETCH_REQUESTS_ERROR, error: error };
}

export function fetchRequestsSuccess(data) {
  return { type: FETCH_REQUESTS_SUCCESS, data: data };
}
