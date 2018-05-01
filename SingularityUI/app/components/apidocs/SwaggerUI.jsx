'use es6';

import React, { Component, PropTypes } from 'react';

export default class SwaggerUI extends Component {

  componentDidMount() {
    window.swaggerUi = SwaggerUIBundle({
      dom_id: '#swagger-container',
      url: this.props.url,
      presets: [SwaggerUIBundle.presets.apis],
      defaultModelRendering: 'model',
      supportedSubmitMethods: ['get']
    });
  }

  componentWillUnmount() {
    try {
      delete window.swaggerUi;
    } catch (e) {
      window.swaggerUi = null;
    }
  }

  render() {
    return (
      <div id="swagger-container" />
    );
  }
}

SwaggerUI.propTypes = {
  url: PropTypes.string,
};
