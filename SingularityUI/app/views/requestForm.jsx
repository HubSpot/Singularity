import React from 'react';
import ReactDOM from 'react-dom';

import View from './view';

import { Provider } from 'react-redux';

import RequestFormPage from '../components/requestForm/RequestForm'


class RequestFormView extends View {
  initialize({store, edit}) {
    this.store = store;
    this.edit = edit;
  }

  render() {
    ReactDOM.render(<Provider store={this.store}><RequestFormPage edit={this.edit}/></Provider>, this.el);
  }
}


export default RequestFormView;
