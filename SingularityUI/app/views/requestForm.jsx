import React from 'react';
import ReactDOM from 'react-dom';

import View from './view';

import { Provider } from 'react-redux';

import RequestFormPage from '../components/requestForm/RequestForm'


class RequestFormView extends View {
  initialize({store}) {
    this.store = store;
  }

  render() {
    ReactDOM.render(<Provider store={this.store}><RequestFormPage /></Provider>, this.el);
  }
}


export default RequestFormView;
