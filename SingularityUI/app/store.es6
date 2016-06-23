import { createStore, compose, applyMiddleware } from 'redux';
import thunk from 'redux-thunk';
import logger from 'redux-logger';

import rootReducer from 'reducers';

export default function configureStore(initialState = {}) {
  const middlewares = [thunk];

  if (window.localStorage.enableReduxLogging) {
    middlewares.push(logger());
  }

  const store = createStore(rootReducer, initialState, compose(applyMiddleware.apply(this, middlewares)));

  return store;
}
