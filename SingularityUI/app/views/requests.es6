import ReactView from './reactView';

import React from 'react';
import ReactDOM from 'react-dom';
import { Provider } from 'react-redux';

import RequestsPage from '../components/requests/RequestsPage';

class RequestsView extends ReactView {

  constructor(store, subFilter, searchFilter, updateFilters) {
    super();
    this.store = store;
    this.subFilter = subFilter;
    this.searchFilter = searchFilter;
    this.updateFilters = updateFilters;
  }

  render() {
    ReactDOM.render(
      <Provider store={this.store}>
        <RequestsPage subFilter={this.subFilter} searchFilter={this.searchFilter} updateFilters={this.updateFilters} />
      </Provider>
    , this.el);
  }
}

export default RequestsView;
