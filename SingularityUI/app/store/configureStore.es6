import { createStore, compose, applyMiddleware } from 'redux';
import thunk from 'redux-thunk';
import logger from 'redux-logger';

import rootReducer from '../reducers';

export default function configureStore() {
  if (window.store !== undefined) {
    return window.store;
  }

  const middlewares = [thunk];

  if (window.localStorage.enableReduxLogging) {
    middlewares.push(logger());
  }

  const store = createStore(rootReducer, {}, compose(applyMiddleware.apply(this, middlewares)));
  // look into hot module replacement for reducers using webpack

  // hack so this can be called multiple times
  window.store = store;

  return store;
}
