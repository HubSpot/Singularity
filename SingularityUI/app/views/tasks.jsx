import ReactView from './reactView';

import React from 'react';
import ReactDOM from 'react-dom';
import { Provider } from 'react-redux';

import TasksPage from '../components/tasks/TasksPage';

class TasksView extends ReactView {

  constructor(store, state, requestsSubFilter, searchFilter, updateFilters) {
    super();
    this.store = store;
    this.state = state;
    this.requestsSubFilter = requestsSubFilter;
    this.searchFilter = searchFilter;
    this.updateFilters = updateFilters;
  }

  render() {
    ReactDOM.render(
      <Provider store={this.store}>
        <TasksPage state={this.state} requestsSubFilter={this.requestsSubFilter} searchFilter={this.searchFilter} updateFilters={this.updateFilters} />
      </Provider>
    , this.el);
  }
}

export default TasksView;
