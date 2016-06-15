import fetch from 'isomorphic-fetch';

export default function buildApiAction(actionName, apiPath, opts={}) {
  const ACTION = actionName;
  const STARTED = actionName + '_STARTED';
  const ERROR = actionName + '_ERROR';
  const SUCCESS = actionName + '_SUCCESS';

  let apiPathFunc;

  if (typeof apiPath === 'string') {
    apiPathFunc = () => apiPath;
  } else {
    apiPathFunc = apiPath;
  }

  function trigger(...args) {
    return function (dispatch) {
      dispatch(started());

      return fetch(
          config.apiRoot + apiPathFunc(...args),
          _.extend(
            {credentials: 'include'},
            opts
          )
        )
        .then(response => response.json())
        .then(json => {
          dispatch(success(json));
        })
        .catch(ex => {
          dispatch(error(ex));
        });
    }
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
    trigger,
    started,
    error,
    success
  }
}
