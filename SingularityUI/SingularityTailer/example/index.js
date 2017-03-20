import { AppContainer } from 'react-hot-loader';
import React from 'react';
import ReactDOM from 'react-dom';

import { createStore, combineReducers, applyMiddleware, compose } from 'redux';
import thunk from 'redux-thunk';
import logger from 'redux-logger';
import { Provider } from 'react-redux';

import { default as singularityTailer } from '../src/reducers';

import AppRouter from './AppRouter';

const reducers = combineReducers({ tailer: singularityTailer });

const middleware = [thunk];

if (localStorage.enableReduxLogging && JSON.parse(localStorage.enableReduxLogging)) {
  middleware.push(logger());
}

const store = createStore(reducers, {}, compose(
  applyMiddleware(...middleware),
  window.devToolsExtension ? window.devToolsExtension() : f => f
));

const rootEl = document.getElementById('root');
ReactDOM.render(
  <AppContainer>
    <Provider store={store}>
      <AppRouter />
    </Provider>
  </AppContainer>,
  rootEl
);

if (module.hot) {
  module.hot.accept('./AppRouter', () => {
    // If you use Webpack 2 in ES modules mode, you can
    // use <App /> here rather than require() a <NextApp />.
    const NextAppRouter = require('./AppRouter').default;
    ReactDOM.render(
      <AppContainer>
        <Provider store={store}>
         <NextAppRouter />
        </Provider>
      </AppContainer>,
      rootEl
    );
  });
}
