import View from './view';
import React from 'react';
import ReactDOM from 'react-dom';

export default class ReactView extends View {
  remove() {
      super.remove();
      ReactDOM.unmountComponentAtNode(this.el);
  }
}
