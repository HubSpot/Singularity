import React from 'react';
import ReactDOM from 'react-dom';

import ReactView from './reactView';

import { Provider } from 'react-redux';

import NewDeployFormPage from '../components/newDeployForm/NewDeployForm';

class NewDeployFormView extends ReactView {
  initialize({store, requestId}) {
    this.store = store;
    this.requestId = requestId;
  }

  render() {
    ReactDOM.render(<Provider store={this.store}><NewDeployFormPage requestId={this.requestId} /></Provider>, this.el);
  }
}

export default NewDeployFormView;
