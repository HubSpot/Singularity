import fetch from 'isomorphic-fetch';

export const FETCH_RACKS = 'FETCH_RACKS';
export const FETCH_RACKS_STARTED = 'FETCH_RACKS_STARTED';
export const FETCH_RACKS_ERROR = 'FETCH_RACKS_ERROR';
export const FETCH_RACKS_SUCCESS = 'FETCH_RACKS_SUCCESS';

export function fetchRacks() {
  return function (dispatch) {
    dispatch(fetchRacksStarted());

    return fetch(`${ config.apiRoot }/racks/`, {
      credentials: 'include'
    })
      .then(response => response.json())
      .then(json => {
        dispatch(fetchRacksSuccess(json));
      })
      .catch(ex => {
        dispatch(fetchRacksError(ex));
      });
  };
}

export function fetchRacksStarted() {
  return { type: FETCH_RACKS_STARTED };
}

export function fetchRacksError(error) {
  return { type: FETCH_RACKS_ERROR, error: error };
}

export function fetchRacksSuccess(data) {
  return { type: FETCH_RACKS_SUCCESS, data: data };
}