import React from 'react';
import ReactDOM from 'react-dom';

import RacksPage from '../components/machines/Racks';

import View from './view';

import { Provider } from 'react-redux';


class RacksView extends View {
  initialize(store) {
    this.store = store;
  }

  render() {
    ReactDOM.render(<Provider store={this.store}><RacksPage/></Provider>, this.el);
  }
}


export default RacksView;
