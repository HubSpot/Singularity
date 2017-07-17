import { createStore, compose, applyMiddleware } from 'redux';
import thunk from 'redux-thunk';
import logger from 'redux-logger';
import { routerMiddleware } from 'react-router-redux';

import rootReducer from 'reducers';

export default function configureStore(initialState = {}, browserHistory) {
  const middlewares = [thunk, routerMiddleware(browserHistory)];

  if (window.localStorage.enableReduxLogging) {
    middlewares.push(logger());
  }

  if (window.__REDUX_DEVTOOLS_EXTENSION__) {
    middlewares.push(window.__REDUX_DEVTOOLS_EXTENSION__());
  }

  const store = createStore(rootReducer, initialState, compose(
    applyMiddleware.apply(this, middlewares)
  ));

  return store;
}
