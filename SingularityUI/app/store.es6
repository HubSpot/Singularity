import { createStore, compose, applyMiddleware } from 'redux';
import thunk from 'redux-thunk';
import logger from 'redux-logger'
import { routerMiddleware } from 'react-router-redux';

import rootReducer from 'reducers';

export default function configureStore(initialState = {}, browserHistory) {
  const middlewares = [thunk, routerMiddleware(browserHistory)];

  let composeEnhancers = compose;
  if (window.localStorage.enableReduxLogging) {
    if (logger) middlewares.push(logger());
  }
  if (window.localStorage.enableReduxExtension) {
    if (window.__REDUX_DEVTOOLS_EXTENSION_COMPOSE__) composeEnhancers = window.__REDUX_DEVTOOLS_EXTENSION_COMPOSE__;
  }

  const store = createStore(rootReducer, initialState, composeEnhancers(
    applyMiddleware.apply(this, middlewares)
  ));

  return store;
}
