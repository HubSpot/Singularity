import { createStore, compose, applyMiddleware } from 'redux';
import thunk from 'redux-thunk';
import logger from 'redux-logger';

import rootReducer from 'reducers';

export default function configureStore(initialState = {}) {
  const middlewares = [thunk];

  if (window.localStorage.enableReduxLogging) {
    middlewares.push(logger());
  }

  const enhancer = compose(
    applyMiddleware.apply(this, middlewares)
  );

  return createStore(rootReducer, initialState, enhancer);
}
