import React, { Component } from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
import rootComponent from '../../rootComponent';

import SwaggerUi, {presets} from 'swagger-ui';
import 'swagger-ui/dist/swagger-ui.css';

class ApiDocs extends Component {

  componentDidMount() {
    SwaggerUi({
      dom_id: '#swaggerContainer',
      url: `${config.apiRoot}/openapi.json`,
      presets: [presets.apis],
      defaultModelRendering: 'model',
      supportedSubmitMethods: ['get']
    });
  }

  render() {
    return (
      <div id="swaggerContainer" />
    );
  }
}

export default ApiDocs;