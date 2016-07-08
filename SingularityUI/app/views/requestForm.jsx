import React from 'react';
import ReactDOM from 'react-dom';

import ReactView from './reactView';

import { Provider } from 'react-redux';

import RequestFormPage from '../components/requestForm/RequestForm';


class RequestFormView extends ReactView {
  initialize({store, requestId}) {
    this.store = store;
    this.requestId = requestId;
  }

  render() {
    ReactDOM.render(<Provider store={this.store}><RequestFormPage requestId={this.requestId} /></Provider>, this.el);
  }
}


export default RequestFormView;
