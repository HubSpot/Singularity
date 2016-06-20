import fetch from 'isomorphic-fetch';

const JSON_HEADERS = {'Content-Type': 'application/json', 'Accept': 'application/json'};

export default function buildJsonSendingApiAction(actionName, httpMethod, opts={}) {
  const jsonBoilerplate = {
    method: httpMethod,
    headers: JSON_HEADERS
  }
  let optsFunctionOrObject;
  if (typeof opts === 'function') {
    optsFunctionOrObject = (...args) => {
      let optsFunctionResult = _.extend({}, jsonBoilerplate, opts(...args));
      if (optsFunctionResult.data) {
        optsFunctionResult.body = JSON.stringify(optsFunctionResult.data);
      }
      return optsFunctionResult;
    }
  } else {
    optsFunctionOrObject = _.extend({}, jsonBoilerplate, opts);
    if (optsFunctionOrObject.data) {
      optsFunctionOrObject.body = JSON.stringify(optsFunctionOrObject.data);
    }
  }

  return buildApiAction(actionName, optsFunctionOrObject);
}

export default function buildApiAction(actionName, opts={}) {
  const ACTION = actionName;
  const STARTED = actionName + '_STARTED';
  const ERROR = actionName + '_ERROR';
  const SUCCESS = actionName + '_SUCCESS';
  const CLEAR = actionName + '_CLEAR';

  let optsFunc;

  if (typeof opts === 'function') {
    optsFunc = opts;
  } else {
    optsFunc = () => opts;
  }

  function trigger(...args) {
    return function (dispatch) {
      dispatch(started());

      let options = optsFunc(...args);
      return fetch(config.apiRoot + options.url, _.extend({credentials: 'include'}, _.omit(options, 'url')))
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
