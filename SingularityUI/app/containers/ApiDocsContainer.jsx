'use es6';

import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';

import SwaggerUI from '../components/apidocs/SwaggerUI';

class ApiDocsContainer extends Component {
  render() {
    return <SwaggerUI url={`${config.apiRoot}/openapi.json`} />;
  }
}

export default connect()(ApiDocsContainer);