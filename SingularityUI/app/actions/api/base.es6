import fetch from 'isomorphic-fetch';
import Messenger from 'messenger';
import Utils from '../../utils';

const JSON_HEADERS = {'Content-Type': 'application/json', 'Accept': 'application/json'};

export function buildApiAction(actionName, opts = {}, keyFunc = undefined) {
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

  function started(key = undefined) {
    return { type: STARTED, key };
  }

  function error(err, options, apiResponse, key = undefined) {
    const action = { type: ERROR, error: err, key, statusCode: apiResponse.status };
    if (Utils.isIn(apiResponse.status, options.catchStatusCodes) || apiResponse.status === 404 && options.renderNotFoundIf404) {
      return action;
    }
    if (apiResponse.status === 502) { // Singularity is deploying
      Messenger().info({
        message: 'Singularity is deploying, your requests cannot be handled. Things should resolve in a few seconds so just hang tight!'
      });
    } else if (apiResponse.status === 401 && config.redirectOnUnauthorizedUrl) { // Redirect to login
      window.location.href = config.redirectOnUnauthorizedUrl.replace('{URL}', encodeURIComponent(window.location.href));
    } else { // Something else happened, display the error
      Messenger().post({
        message: `<p>An error occurred while accessing <code>${options.url}</code></p><pre>${err}</pre>`,
        type: 'error'
      });
    }

    return action;
  }

  function success(data, statusCode, key = undefined) {
    return { type: SUCCESS, data, statusCode, key };
  }

  function clearData() {
    return (dispatch) => {
      dispatch(clear());
    };
  }

  function trigger(...args) {
    return (dispatch) => {
      let key;
      if (keyFunc) {
        key = keyFunc(...args);
      }
      dispatch(started(key));

      const options = optsFunc(...args);
      let apiResponse;
      let userParam = '';
      if (localStorage.getItem('singularityUserId')) {
        if (options.url.includes('?')) {
          userParam = `&user=${localStorage.getItem('singularityUserId')}`
        } else {
          userParam = `?user=${localStorage.getItem('singularityUserId')}`
        }
      }

      if (config.generateAuthHeader) {
        options.headers = options.headers || {};
        options.headers.Authorization = Utils.getAuthTokenHeader();
      }

      return fetch(config.apiRoot + options.url + userParam, _.extend({credentials: 'include'}, _.omit(options, 'url')))
        .then(response => {
          apiResponse = response;
          if (response.status === 204) {
            return Promise.resolve();
          }
          if (response.headers.get('Content-Type') === 'application/json') {
            return response.json();
          }
          return response.text();
        })
        .then((data) => {
          if (apiResponse.status >= 200 && apiResponse.status < 300) {
            return dispatch(success(data, apiResponse.status, key));
          }
          if (data.message) {
            return dispatch(error(data.message, options, apiResponse, key));
          }
          return dispatch(error(data, options, apiResponse, key));
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

export function buildJsonApiAction(actionName, httpMethod, opts = {}, keyField = undefined) {
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

  return buildApiAction(actionName, options, keyField);
}
