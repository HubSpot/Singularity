import fetch from 'isomorphic-fetch';

const JSON_HEADERS = {'Content-Type': 'application/json', 'Accept': 'application/json'};

export function buildJsonApiAction(actionName, httpMethod, opts={}, keyField=undefined) {
  const JSON_BOILERPLATE = {
    method: httpMethod,
    headers: JSON_HEADERS
  }

  let options;
  if (typeof opts === 'function') {
    options = (...args) => {
      let generatedOpts = opts(...args);
      generatedOpts.body = JSON.stringify(generatedOpts.body || {});
      return _.extend({}, generatedOpts, JSON_BOILERPLATE);
    };
  } else {
    options = (...args) => {
      opts.body = JSON.stringify(opts.body || {});
      return _.extend({}, opts, JSON_BOILERPLATE);
    };
  }

  return buildApiAction(actionName, options, keyField);
}

export function buildApiAction(actionName, opts={}, keyFunc=undefined) {
  const ACTION = actionName;
  const STARTED = `${actionName}_STARTED`;
  const ERROR = `${actionName}_ERROR`;
  const SUCCESS = `${actionName}_SUCCESS`;
  const CLEAR = `${actionName}_CLEAR`;

  let optsFunc;

  if (typeof opts === 'function') {
    optsFunc = opts;
  } else {
    optsFunc = () => opts;
  }

  function trigger(...args) {
    return function (dispatch) {
      let key;
      if (keyFunc) {
        key = keyFunc(...args);
      }
      dispatch(started(key));

      const options = optsFunc(...args);
      let apiResponse;
      return fetch(config.apiRoot + options.url, _.extend({credentials: 'include'}, _.omit(options, 'url')))
        .then(response => {
          apiResponse = response;
          if (response.headers.get('Content-Type') === 'application/json') {
            return response.json();
          }
          return response.text();
        })
        .then((data) => {
          if (apiResponse.status >= 200 && apiResponse.status < 300) {
            return dispatch(success(data, key));
          }
          throw new Error(data);
        })
        .catch(ex => {
          return dispatch(error(ex, key));
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

  function started(key=undefined) {
    return { type: STARTED, key };
  }

  function error(error, key=undefined) {
    return { type: ERROR, error, key };
  }

  function success(data, key=undefined) {
    return { type: SUCCESS, data, key };
  }

  function clearData() {
    return function (dispatch) {
      dispatch(clear());
    };
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
  };
}
