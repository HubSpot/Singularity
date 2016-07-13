import ReactView from './reactView';
import NotFound from '../components/common/NotFound';

import React from 'react';
import ReactDOM from 'react-dom';

class NotFoundView extends ReactView {
  render() {
    ReactDOM.render(<NotFound path={Backbone.history.fragment} />, this.el);
  }
}

export default NotFoundView;
