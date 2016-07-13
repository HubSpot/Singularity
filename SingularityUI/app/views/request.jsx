import ReactView from './reactView';
import RequestDetailPage from '../components/requestDetail/RequestDetailPage';

import React from 'react';
import ReactDOM from 'react-dom';

import { Provider } from 'react-redux';

class RequestDetailView extends ReactView {
  initialize({store, requestId}) {
    this.store = store;
    this.requestId = requestId;
  }

  render() {
    ReactDOM.render(<Provider store={this.store}><RequestDetailPage requestId={this.requestId} /></Provider>, this.el);
  }
}

export default RequestDetailView;
