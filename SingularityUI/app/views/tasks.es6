import View from './view';

import React from 'react';
import ReactDOM from 'react-dom';
import { Provider } from 'react-redux';

import TasksPage from '../components/tasks/TasksPage';

class TasksView extends View {

  constructor(store, state, requestsSubFilter, searchFilter) {
    super();
    this.store = store;
    this.state = state;
    this.requestsSubFilter = requestsSubFilter;
    this.searchFilter = searchFilter;
  }

  render() {
    ReactDOM.render(
      <Provider store={this.store}>
        <TasksPage state={this.state} requestsSubFilter={this.requestsSubFilter} searchFilter={this.searchFilter} />
      </Provider>
    , this.el);
  }
}

export default TasksView;
