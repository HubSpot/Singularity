import ReactView from './reactView';
import React from 'react';
import ReactDOM from 'react-dom';
import LogContainer from '../components/logs/LogContainer';
import { Provider } from 'react-redux';

class LogView extends ReactView {
    constructor(...args) {
      super(...args);
    }

    initialize(store) {
      this.handleViewChange = this.handleViewChange.bind(this);
      window.addEventListener('viewChange', this.handleViewChange);
      this.store = store;
    }

    handleViewChange() {
      let unmounted = ReactDOM.unmountComponentAtNode(this.el);
      if (unmounted) {
        return window.removeEventListener('viewChange', this.handleViewChange);
      }
    }

    render() {
      $(this.el).addClass('tail-root');
      ReactDOM.render(<Provider store={this.store}><LogContainer /></Provider>, this.el);
    }
  }

export default LogView;
