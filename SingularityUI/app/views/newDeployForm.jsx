import React from 'react';
import ReactDOM from 'react-dom';

import View from './view';

import { Provider } from 'react-redux';

import NewDeployFormPage from '../components/newDeployForm/NewDeployForm'

class NewDeployFormView extends View {
  initialize({store}) {
    this.store = store;
  }

  render() {
    ReactDOM.render(<Provider store={this.store}><NewDeployFormPage /></Provider>, this.el);
  }
}



export default NewDeployFormView;
