import React from 'react';
import ReactDOM from 'react-dom';

import RacksPage from '../components/machines/Racks';

import ReactView from './reactView';

import { Provider } from 'react-redux';


class RacksView extends ReactView {
  initialize(store) {
    this.store = store;
  }

  render() {
    ReactDOM.render(<Provider store={this.store}><RacksPage/></Provider>, this.el);
  }
}


export default RacksView;
