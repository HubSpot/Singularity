import View from './view';
import React from 'react';
import ReactDOM from 'react-dom';
import LogContainer from '../components/logs/LogContainer';
import { Provider } from 'react-redux';

class LogView extends View {
    constructor(...args) {
      super(...args);
    }

    initialize(store) {
      this.handleViewChange = this.handleViewChange.bind(this);
      window.addEventListener('viewChange', this.handleViewChange);
      this.component = <Provider store={store}><LogContainer /></Provider>;
    }
      

    handleViewChange() {
      const unmounted = ReactDOM.unmountComponentAtNode(this.el);
      if (unmounted) {
        return window.removeEventListener('viewChange', this.handleViewChange);
      }
    }

    render() {
      $(this.el).addClass('tail-root');
      return ReactDOM.render(this.component, this.el);
    }
  }

export default LogView;
