import fetch from 'isomorphic-fetch';

export default function buildAction(name, apiPath) {
  const FETCH = 'FETCH_' + name;
  const FETCH_STARTED = 'FETCH_' + name + '_STARTED';
  const FETCH_ERROR = 'FETCH_' + name + '_ERROR';
  const FETCH_SUCCESS = 'FETCH_' + name + '_SUCCESS';

  function fetch() {
    return function (dispatch) {
      dispatch(fetchStarted());

      return fetch(config.apiRoot + apiPath, {credentials: 'include'})
        .then(response => response.json())
        .then(json => {
          dispatch(fetchSuccess(json));
        })
        .catch(ex => {
          dispatch(fetchError(ex));
        });
    }
  }

  function fetchStarted() {
    return { type: FETCH_STARTED };
  }

  function fetchError(error) {
    return { type: FETCH_ERROR };
  }

  function fetchSuccess(data) {
    return { type: FETCH_SUCCESS, data };
  }

  return {
    FETCH,
    FETCH_STARTED,
    FETCH_ERROR,
    FETCH_SUCCESS,
    fetch,
    fetchStarted,
    fetchError,
    fetchSuccess
  }
}