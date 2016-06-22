import React from 'react';
import ReactDOM from 'react-dom';

import View from './view';

import { Provider } from 'react-redux';

import RequestFormPage from '../components/requestForm/RequestForm'


class RequestFormView extends View {
  initialize({store, editing}) {
    this.store = store;
    this.editing = editing;
  }

  render() {
    ReactDOM.render(<Provider store={this.store}><RequestFormPage editing={this.editing} /></Provider>, this.el);
  }
}


export default RequestFormView;
