import React, { Component } from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
import rootComponent from '../../rootComponent';
import { refresh } from '../../actions/ui/apidocs';

import SwaggerUi, {presets} from 'swagger-ui';
import 'swagger-ui/dist/swagger-ui.css';

class ApiDocs extends Component {

  componentDidMount() {
    SwaggerUi({
      dom_id: '#swaggerContainer',
      url: this.props.url,
      spec: this.props.spec,
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

function mapStateToProps(state) {
  return {
    url: `${config.apiRoot}/openapi.json`,
    spec: state.api.apidocs.data
  };
}

ApiDocs.propTypes = {
  url: PropTypes.string,
  spec: PropTypes.object
};

export default connect(mapStateToProps)(rootComponent(ApiDocs, refresh));