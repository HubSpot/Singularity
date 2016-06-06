import fetch from 'isomorphic-fetch';

export const FETCH_REQUESTS = 'FETCH_REQUESTS';
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
  }
}

export function fetchRequestsStarted() {
  return { type: FETCH_REQUESTS };
}

export function fetchRequestsError(error) {
  return { type: FETCH_REQUESTS, status: 'error', error: error };
}

export function fetchRequestsSuccess(data) {
  return { type: FETCH_REQUESTS, status: 'success', data: data };
}
