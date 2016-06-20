import fetch from 'isomorphic-fetch';

export default function buildApiAction(actionName, apiPath, opts={}) {
  const ACTION = actionName;
  const STARTED = actionName + '_STARTED';
  const ERROR = actionName + '_ERROR';
  const SUCCESS = actionName + '_SUCCESS';
  const CLEAR = actionName + '_CLEAR';

  let apiPathFunc;
  let optsFunc;

  if (typeof apiPath === 'string') {
    apiPathFunc = () => apiPath;
  } else {
    apiPathFunc = apiPath;
  }

  if (typeof opts === 'object') {
    optsFunc = () => opts;
  } else {
    optsFunc = opts;
  }

  function trigger(...args) {
    return function (dispatch) {
      dispatch(started());

      return fetch(config.apiRoot + apiPathFunc(...args), _.extend({credentials: 'include'}, optsFunc(...args)))
        .then(response => response.json())
        .then(json => {
          dispatch(success(json));
        })
        .catch(ex => {
          dispatch(error(ex));
        });
    }
  }

  function clearData() {
    return function (dispatch) {
      dispatch(clear());
    }
  }

  function clear() {
    return { type: CLEAR };
  }

  function started() {
    return { type: STARTED };
  }

  function error(error) {
    return { type: ERROR, error };
  }

  function success(data) {
    return { type: SUCCESS, data };
  }

  return {
    ACTION,
    STARTED,
    ERROR,
    SUCCESS,
    CLEAR,
    clear,
    clearData,
    trigger,
    started,
    error,
    success
  }
}
