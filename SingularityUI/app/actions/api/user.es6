import fetch from 'isomorphic-fetch';

export const FETCH_USER = 'FETCH_REQUESTS';
export const FETCH_USER_STARTED = 'FETCH_USER_STARTED';
export const FETCH_USER_ERROR = 'FETCH_USER_ERROR';
export const FETCH_USER_SUCCESS = 'FETCH_USER_SUCCESS';

export function fetchUser() {
  return function (dispatch) {
    dispatch(fetchUserStarted());

    return fetch(`${ config.apiRoot }/auth/user/`, {
      credentials: 'include'
    })
      .then(response => response.json())
      .then(json => {
        dispatch(fetchUserSuccess(json));
      })
      .catch(ex => {
        dispatch(fetchUserError(ex));
      });
  };
}

export function fetchUserStarted() {
  return { type: FETCH_USER_STARTED };
}

export function fetchUserError(error) {
  return { type: FETCH_USER_ERROR, error: error };
}

export function fetchUserSuccess(data) {
  return { type: FETCH_USER_SUCCESS, data: data };
}