import React from 'react';
import ReactDOM from 'react-dom';

import SlavesPage from '../components/machines/Slaves';

import View from './view';

import { Provider } from 'react-redux';


class SlavesView extends View {
  initialize(store) {
    this.store = store;
  }

  render() {
    ReactDOM.render(<Provider store={this.store}><SlavesPage/></Provider>, this.el);
  }
}

export default SlavesView;
