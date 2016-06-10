import fetch from 'isomorphic-fetch';

export const FETCH_WEBHOOKS = 'FETCH_WEBHOOKS';
export const FETCH_WEBHOOKS_STARTED = 'FETCH_WEBHOOKS_STARTED';
export const FETCH_WEBHOOKS_ERROR = 'FETCH_WEBHOOKS_ERROR';
export const FETCH_WEBHOOKS_SUCCESS = 'FETCH_WEBHOOKS_SUCCESS';

export function fetchWebhooks() {
  return function (dispatch) {
    dispatch(fetchWebhooksStarted());

    return fetch(`${ config.apiRoot }/webhooks/`, {
      credentials: 'include'
    })
      .then(response => response.json())
      .then(json => {
        dispatch(fetchWebhooksSuccess(json));
      })
      .catch(ex => {
        dispatch(fetchWebhooksError(ex));
      });
  };
}

export function fetchWebhooksStarted() {
  return { type: FETCH_WEBHOOKS_STARTED };
}

export function fetchWebhooksError(error) {
  return { type: FETCH_WEBHOOKS_ERROR, error: error };
}

export function fetchWebhooksSuccess(data) {
  return { type: FETCH_WEBHOOKS_SUCCESS, data: data };
}