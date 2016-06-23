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

  // set up subscriber
  store.subscribe(() => {
    const starredRequests = store.getState().ui.starred;
    window.localStorage.starredRequests = JSON.stringify(starredRequests);
  });

  return store;
}
