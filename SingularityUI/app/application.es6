import React from 'react';
import ReactDOM from 'react-dom';
import AppRouter from './router';
import configureStore from 'store';
import { FetchUser } from 'actions/api/auth';
import Utils from './utils';

class Application {
  initialize() {
    // set up Redux store
    this.store = configureStore();

    // set up user
    this.store.dispatch(FetchUser.trigger());

    // hide loading animation
    $('.page-loader.fixed').hide();

    // Render the page content
    ReactDOM.render(<AppRouter store={this.store} />, document.getElementById('root'));
  }
}

export default new Application;
