import View from './view';
import React from 'react';
import ReactDOM from 'react-dom';
import RequestsPage from '../components/requests/RequestsPage';
import { Provider } from 'react-redux';

class RequestsView extends View {
    constructor(...args) {
      super(...args);
    }

    initialize(store) {
      // backbone calls this before the constructor can get to do its thing
      this.handleViewChange = this.handleViewChange.bind(this);
      this.render = this.render.bind(this);
      window.addEventListener('viewChange', this.handleViewChange);
      this.component = (
        <Provider store={store}>
          <RequestsPage />
        </Provider>
      );
    }

    handleViewChange() {
      const unmounted = ReactDOM.unmountComponentAtNode(this.el);
      if (unmounted) {
        return window.removeEventListener('viewChange', this.handleViewChange);
      }
    }

    render() {
      $(this.el).addClass('requests-root');
      return ReactDOM.render(this.component, this.el);
    }
  }

export default RequestsView;
