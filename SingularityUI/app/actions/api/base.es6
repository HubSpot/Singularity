import fetch from 'isomorphic-fetch';

const JSON_HEADERS = {'Content-Type': 'application/json', 'Accept': 'application/json'};

export function buildJsonApiAction(actionName, httpMethod, opts = {}) {
  const JSON_BOILERPLATE = {
    method: httpMethod,
    headers: JSON_HEADERS
  };

  let options;
  if (typeof opts === 'function') {
    options = (...args) => {
      const generatedOpts = opts(...args);
      generatedOpts.body = JSON.stringify(generatedOpts.body || {});
      return _.extend({}, generatedOpts, JSON_BOILERPLATE);
    };
  } else {
    options = () => {
      opts.body = JSON.stringify(opts.body || {});
      return _.extend({}, opts, JSON_BOILERPLATE);
    };
  }

  return buildApiAction(actionName, options);
}

export function buildApiAction(actionName, opts = {}) {
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

  function clearData() {
    return function (dispatch) {
      dispatch(clear());
    };
  }

  function trigger(...args) {
    return function (dispatch) {
      dispatch(started());

      const options = optsFunc(...args);
      return fetch(config.apiRoot + options.url, _.extend({credentials: 'include'}, _.omit(options, 'url')))
        .then(response => {
          if (response.status >= 200 && response.status < 300) {
            if (response.headers.get('Content-Type') === 'application/json') {
              return response.json().then(json => dispatch(success(json)));
            }
            return response.text().then(body => dispatch(success({response: body})));
          }
          if (response.headers.get('Content-Type') === 'application/json') {
            return response.json().then(body => dispatch(error(body)));
          }
          return response.text().then(body => dispatch(error({message: body})));
        })
        .catch(ex => {
          dispatch(error(ex));
        });
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
