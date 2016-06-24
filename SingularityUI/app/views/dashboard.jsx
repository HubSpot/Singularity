import ReactView from './reactView';
import DashboardPage from '../components/dashboard/DashboardPage';

import React from 'react';
import ReactDOM from 'react-dom';

import { Provider } from 'react-redux';

class DashboardView extends ReactView {
  initialize(store) {
    this.store = store;
  }

  render() {
    ReactDOM.render(<Provider store={this.store}><DashboardPage /></Provider>, this.el);
  }
}

export default DashboardView;
