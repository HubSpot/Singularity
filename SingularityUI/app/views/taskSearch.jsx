import ReactView from './reactView';
import TaskSearch from '../components/taskSearch/TaskSearch';

import React from 'react';
import ReactDOM from 'react-dom';

import { Provider } from 'react-redux';

export default class TaskSearchView extends ReactView {

  constructor(store, requestId) {
    super();
    this.store = store;
    this.requestId = requestId;
  }

  render() {
    ReactDOM.render(<Provider store={this.store}><TaskSearch requestId={this.requestId} /></Provider>, this.el);
  }
}
